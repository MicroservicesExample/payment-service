package org.ashok.paymentservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.ashok.paymentservice.config.SecurityConfig;
import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentService;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;


@WebFluxTest(PaymentController.class)
@Import(SecurityConfig.class)
//@AutoConfigureWebTestClient(timeout = "3600000") // uncomment for debugging
//TODO - For now, disabled this test , please make it work
@Disabled
class PaymentControllerWebFluxTests {

	@Autowired
	WebTestClient webTestClient;
	
	@MockBean
	PaymentService service;
	
	@MockBean
	ReactiveJwtDecoder reactiveJwtDecoder; //mocks reactiveJwtDecoder so that it does not call auth service to get public key to decode the token
	
	@Test
	void whenInvoiceNotAvailableThenRejectPayment() {
		var paymentRequest = new PaymentRequest(123456L, 1000);
		
		var expectedPayment = PaymentService.buildRejectedPayment(paymentRequest.billRefNumber(), paymentRequest.amount());
		
		given(service.makePayment(paymentRequest.billRefNumber(), paymentRequest.amount(), null))
			.willReturn(Mono.just(expectedPayment));
		
		webTestClient
			.mutateWith(SecurityMockServerConfigurers 
					.mockJwt() //mock jwt token with Role_user
					.authorities(new SimpleGrantedAuthority("ROLE_user"))
					)
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
