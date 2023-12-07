package org.ashok.paymentservice.persistence;

import java.time.LocalDate;
import java.util.Objects;

import org.ashok.paymentservice.config.DataConfig;
import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentService;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.ashok.paymentservice.invoice.Invoice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.test.StepVerifier;



@DataR2dbcTest
@Import(DataConfig.class)
@Testcontainers //Activates automatic startup and cleanup of test containers
class PaymentRepositoryR2DBCTest {

	@Container
	 private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.4")
	   .withDatabaseName("paymentService").withUsername("postgres").withPassword("postgres");
	 
	static {
		postgreSQLContainer.start();
	}
	
	
	@Autowired
	PaymentRepository repository;
		
	
	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.driver-class-name",() -> "org.postgresql.Driver");
		registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
		registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
		registry.add("spring.datasource.password", postgreSQLContainer::getPassword);				
	}
	
	
	@Test
	void createRejectedPayment() {
		
		Payment rejectedPayment = PaymentService.buildRejectedPayment(45L, 100);
		StepVerifier
			.create(repository.save(rejectedPayment))
			.expectNextMatches(payment -> payment.status().equals(PaymentStatus.REJECTED))
			.verifyComplete();		
		
	}
	
	@Test
	void createAcceptedPaymentWhenUnauthenticatedUserThenNoAuditUserData() {
		
		Invoice invoice = new Invoice(123L,"testUser",1000, LocalDate.now());
		Payment acceptedPayment = PaymentService.buildAcceptedPayment(invoice, 1000);
		
		StepVerifier
		.create(repository.save(acceptedPayment))
		.expectNextMatches(payment -> 
							payment.status().equals(PaymentStatus.ACCEPTED)
							&& Objects.isNull(payment.createdBy())
							&& Objects.isNull(payment.lastModifiedBy())
		  				)
		.verifyComplete();		
		
	}
	
	@Test
	@WithMockUser("testUser")
	void createAcceptedPaymentWhenAuthenticatedUserThenUserAuditDataIsPresent() {
		
		Invoice invoice = new Invoice(123L,"test",1000, LocalDate.now());
		Payment acceptedPayment = PaymentService.buildAcceptedPayment(invoice, 1000);
		
		StepVerifier
		.create(repository.save(acceptedPayment))
		.expectNextMatches(payment -> 
							payment.status().equals(PaymentStatus.ACCEPTED)
							&& payment.createdBy().equals("testUser")
							&& payment.lastModifiedBy().equals("testUser")
		  				)
		.verifyComplete();		
		
	}

}
