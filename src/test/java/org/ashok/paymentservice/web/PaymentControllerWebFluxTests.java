package org.ashok.paymentservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentService;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;


@WebFluxTest(PaymentController.class)
class PaymentControllerWebFluxTests {

	@Autowired
	WebTestClient webTestClient;
	
	@MockBean
	PaymentService service;
	
	
	
	@Test
	void whenInvoiceNotAvailableThenRejectPayment() {
		var paymentRequest = new PaymentRequest(123456L, 1000);
		
		var expectedPayment = PaymentService.buildRejectedPayment(paymentRequest.billRefNumber(), paymentRequest.amount());
		
		given(service.makePayment(paymentRequest.billRefNumber(), paymentRequest.amount()))
			.willReturn(Mono.just(expectedPayment));
		
		webTestClient
			.post()
			.uri("/payments")
			.bodyValue(paymentRequest)
			.exchange()
			.expectStatus().is2xxSuccessful()
			.expectBody(Payment.class)
				.value(actualPayment -> {
					assertThat(actualPayment).isNotNull();
					assertThat(actualPayment.status()).isEqualTo(PaymentStatus.REJECTED);				
				});
	}

}
