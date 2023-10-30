package org.ashok.paymentservice.invoice;

import java.time.LocalDate;

public record Invoice(		
		
		Long id,
		String userId,
		Integer amount,
		LocalDate dueDate
	) {}
		