package org.ashok.paymentservice.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Payment(
		
	@Id
	Long id,
	
	
	@NotBlank(message="Bill reference number is required")
	Long billRefNumer,
	
	@NotBlank(message="user id must be defined")
	@Email(message="must be valid email address")
	String userId,
	
	@NotNull(message="Bill amount must be defined")
	@Positive(message="Bill amount should be positive")
	Integer billAmount,
	
	@NotNull(message="Bill amount must be defined")
	@Positive(message="Bill amount should be positive")
	Integer paymentAmount,
	
	
	PaymentStatus status,
	
	@Version // concurreny handling
	int version,
	
	@CreatedDate
	LocalDateTime createdDate,
	
	@LastModifiedDate
	LocalDateTime lastModifiedDate)
	{
		public static Payment of(Long billRefNum, String userId, Integer billAmount, Integer paymentAmount, PaymentStatus status) {
			return new Payment(null, billRefNum, userId, billAmount, paymentAmount, status, 0, null, null);
		}

	}
