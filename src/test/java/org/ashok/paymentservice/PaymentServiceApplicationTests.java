package org.ashok.paymentservice;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.ashok.paymentservice.invoice.Invoice;
import org.ashok.paymentservice.invoice.InvoiceClient;
import org.ashok.paymentservice.web.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
/**
 * @SpringBootTest: Will load entire application context and will start the servlet container on random port
 * @author Ashok Mane
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testdata")
class PaymentServiceApplicationTests {

	@Autowired
	WebTestClient webTestClient;
	
	@MockBean
	InvoiceClient invoiceClient;
	
	
	
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
			.uri("/payments/" + request.billRefNumber())
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
	void whenPostPaymentsAndInvoiceExistsThenPaymentAccepted() {
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
