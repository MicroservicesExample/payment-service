package org.ashok.paymentservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.time.LocalDate;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.ashok.paymentservice.event.PaymentMessage;
import org.ashok.paymentservice.invoice.Invoice;
import org.ashok.paymentservice.invoice.InvoiceClient;
import org.ashok.paymentservice.web.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
/**
 * @SpringBootTest: Will load entire application context and will start the servlet container on random port
 * @author Ashok Mane
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("testdata")
class PaymentServiceApplicationTests {

	@Autowired
	WebTestClient webTestClient;
	
	@MockBean
	InvoiceClient invoiceClient;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OutputDestination output; // mapped to paymentAccepted-out-0
	
	@Test
	void whenGetPaymentsThenReturn() {
		//1. set up payment in db first
		PaymentRequest request = new PaymentRequest(567812L, 200);
				
		Invoice invoice = new Invoice(request.billRefNumber(), "test@gmail.com", request.amount(), LocalDate.now());
		
		given(invoiceClient.getInvoiceById(request.billRefNumber()))
			.willReturn(Mono.just(invoice));
		
		Payment expectedPayment = webTestClient
									.post()
									.uri("/payments")
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
		
		given(invoiceClient.getInvoiceById(request.billRefNumber()))
			.willReturn(Mono.just(invoice));
		
		Payment expectedPayment = webTestClient
									.post()
									.uri("/payments")
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
						
		given(invoiceClient.getInvoiceById(request.billRefNumber()))
			.willReturn(Mono.empty());
		
		Payment expectedPayment = webTestClient
									.post()
									.uri("/payments")
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
	

}
