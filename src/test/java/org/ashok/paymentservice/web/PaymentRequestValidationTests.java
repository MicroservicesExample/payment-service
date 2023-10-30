package org.ashok.paymentservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PaymentRequestValidationTests {

	private static Validator validator;
	
	
	@BeforeAll
	static void setup() {
		ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}
	
	
	
	
	@Test
	void validationSuccess() {
		PaymentRequest request = new PaymentRequest(1234512L, 300);
		Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
		assertThat(violations).isEmpty();
	}
	
	@Test
	void validationFailsIfAmoutIsNotPositive() {
		PaymentRequest request = new PaymentRequest(1234512L, 0);
		Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
		assertThat(violations).hasSize(1);
	}
	
	@Test
	void validationFailsIfBillRefNumIsNull() {
		PaymentRequest request = new PaymentRequest(null, 100);
		Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
		assertThat(violations).hasSize(1);
	}

}
