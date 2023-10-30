package org.ashok.paymentservice.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
	
		@NotNull(message="bill reference number is required")
		Long billRefNumber,
		
		@NotNull(message="amount is required")
		@Positive(message="amount should be positive number")
		Integer amount
		
		) 
{

}
