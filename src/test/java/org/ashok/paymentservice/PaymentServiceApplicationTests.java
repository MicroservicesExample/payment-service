package org.ashok.paymentservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.ashok.paymentservice.event.PaymentMessage;
import org.ashok.paymentservice.invoice.Invoice;
import org.ashok.paymentservice.invoice.InvoiceClient;
import org.ashok.paymentservice.web.PaymentRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
/**
 * @SpringBootTest: Will load entire application context and will start the servlet container on random port
 * @author Ashok Mane
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers //Activates automatic startup and cleanup of test containers
class PaymentServiceApplicationTests {

private static final DockerImageName AUTH_SERVICE_IMAGE = DockerImageName.parse("ghcr.io/microservicesexample/auth-service:latest");
	
	@Container
	 private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.4")
	   .withDatabaseName("paymentservice").withUsername("postgres").withPassword("postgres");
	 
	static {
		postgreSQLContainer.start();
	}
	
	@Container
	private static final GenericContainer<?> authServiceContainer;
	static {
		authServiceContainer = new GenericContainer<>(AUTH_SERVICE_IMAGE)
	    .withEnv(
	    			Map.of(
	    					"app.client.registration.client-id", "test",
	    					"app.client.registration.client-secret","{noop}testsecret",
	    					"spring.profiles.active", "testdata" //h2 db
	    					
	     				)
	    		)
		.withExposedPorts(9000)
		.withStartupCheckStrategy(
				new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(20))
			)
	    
		.waitingFor(Wait.forHttp("/"));
		
		authServiceContainer.start();
	//	final String logs = authServiceContainer.getLogs();
	//	System.out.println("Container logs ---------"+ logs);
		

		
	}
	
	private static AccessToken userAccessToken;
	
	
	@Autowired
	WebTestClient webTestClient;
	
	@MockBean
	InvoiceClient invoiceClient;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OutputDestination output; // mapped to paymentAccepted-out-0
	
	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.driver-class-name",() -> "org.postgresql.Driver");
		registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
		registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
		registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
		
		
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
						() -> getAuthServiceContainerUrl());
	}
	
	
	static String getAuthServiceContainerUrl() {
		return "http://" + authServiceContainer.getHost() + ":" + authServiceContainer.getMappedPort(9000);
	}

	@BeforeAll
	static void generateAccessToken() {
		WebClient webClient = WebClient.builder()
								.baseUrl(getAuthServiceContainerUrl()+"/oauth2/token")
								.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
								.build();
		
		userAccessToken = authenticateClient(webClient, "test", "testsecret");
		System.out.println("****userAccessToken:" + userAccessToken);
	//	final String logs = authServiceContainer.getLogs();
	//	System.out.println("Container logs ---------"+ logs);
	}
	
	static AccessToken authenticateClient(WebClient webClient, String clientId, String clientSecret) {
		
		AccessToken response = 
		    webClient
				.post()
				.headers(httpHeaders -> setAuthorizationHeader(httpHeaders, clientId, clientSecret))
				.body(
						BodyInserters.fromFormData("grant_type", "client_credentials")
																
					)
				.retrieve()
				.bodyToMono(AccessToken.class)
				.block();
				
		System.out.println("*******access token response: " + response);
		return response;
	}

	static String getAuthorizationHeader() {
		return "Bearer " + userAccessToken.token();
	}
	
	private static void setAuthorizationHeader(HttpHeaders httpHeaders, String clientId, String clientSecret) {
		String headerValue = clientId + ":" + clientSecret;
		httpHeaders.add("Authorization", "Basic " + Base64.getEncoder().encodeToString(headerValue.getBytes()) );
	}

	
	@Test
	void whenGetPaymentsThenReturn() {
		//1. set up payment in db first
		PaymentRequest request = new PaymentRequest(567812L, 200);
				
		Invoice invoice = new Invoice(request.billRefNumber(), "test@gmail.com", request.amount(), LocalDate.now());
		
		given(invoiceClient.getInvoiceById(request.billRefNumber(), getAuthorizationHeader()))
			.willReturn(Mono.just(invoice));
		
		Payment expectedPayment = webTestClient
									.post()
									.uri("/payments")
									.headers(headers -> headers.setBearerAuth(userAccessToken.token()))
									.bodyValue(request)
									.exchange()
									.expectStatus().isOk()
									.expectBody(Payment.class)
									.returnResult()
									.getResponseBody();
		
		assertThat(expectedPayment).isNotNull();
		
		//2. get the payment
		webTestClient
			.get()
			.uri("/payments/" + expectedPayment.id())
			.headers(headers -> headers.setBearerAuth(userAccessToken.token()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Payment.class).value( payments -> {
				payments
				.stream()
				.filter(payment -> payment.billRefNumer().equals(request.billRefNumber()))
				.findAny().isPresent();
			});
	}
	
	@Test
	void whenPostPaymentsAndInvoiceExistsThenPaymentAccepted() throws IOException {
		PaymentRequest request = new PaymentRequest(1212121L, 200);
		
		Invoice invoice = new Invoice(request.billRefNumber(), "test@gmail.com", request.amount(), LocalDate.now());
		
		given(invoiceClient.getInvoiceById(request.billRefNumber(), getAuthorizationHeader()))
			.willReturn(Mono.just(invoice));
		
		Payment expectedPayment = webTestClient
									.post()
									.uri("/payments")
									.headers(headers -> headers.setBearerAuth(userAccessToken.token()))
									.bodyValue(request)
									.exchange()
									.expectStatus().isOk()
									.expectBody(Payment.class)
									.returnResult()
									.getResponseBody();
		
		assertThat(expectedPayment).isNotNull();
		assertThat(expectedPayment.billRefNumer()).isEqualTo(request.billRefNumber());
		assertThat(expectedPayment.status()).isEqualTo(PaymentStatus.ACCEPTED);
		//check that it put the message on notify-user channel/exchange
		assertThat(objectMapper.readValue(output.receive().getPayload(), PaymentMessage.class))
		.isEqualTo(new PaymentMessage(expectedPayment.id(), expectedPayment.billRefNumer(), expectedPayment.paymentAmount()));
	}
	
	@Test
	void whenPostPaymentsAndInvoiceDoesNotExistThenPaymentRejected() {
		PaymentRequest request = new PaymentRequest(123321L, 200);
						
		given(invoiceClient.getInvoiceById(request.billRefNumber(), getAuthorizationHeader()))
			.willReturn(Mono.empty());
		
		Payment expectedPayment = webTestClient
									.post()
									.uri("/payments")
									.headers(headers -> headers.setBearerAuth(userAccessToken.token()))
									.bodyValue(request)
									.exchange()
									.expectStatus().isOk()
									.expectBody(Payment.class)
									.returnResult()
									.getResponseBody();
		
		assertThat(expectedPayment).isNotNull();
		assertThat(expectedPayment.billRefNumer()).isEqualTo(request.billRefNumber());
		assertThat(expectedPayment.status()).isEqualTo(PaymentStatus.REJECTED);
		
	}
	
	private record AccessToken(String token) {
		
		@JsonCreator
		private AccessToken(@JsonProperty("access_token") final String token){
			this.token = token;
		}
	}

}
