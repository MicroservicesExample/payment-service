package org.ashok.paymentservice.persistence;

import org.ashok.paymentservice.config.DataConfig;
import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentService;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;



@DataR2dbcTest
@Import(DataConfig.class)
@ActiveProfiles("testdata")
class PaymentRepositoryR2DBCTest {

	@Autowired
	PaymentRepository repository;
		
	
	@Test
	void createRejectedPayment() {
		
		Payment rejectedPayment = PaymentService.buildRejectedPayment(45L, 100);
		StepVerifier
			.create(repository.save(rejectedPayment))
			.expectNextMatches(payment -> payment.status().equals(PaymentStatus.REJECTED))
			.verifyComplete();		
		
	}

}
