package org.ashok.paymentservice.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix="config")
public record ClientProperties(
		
		@NotNull
		URI invoiceServiceUri	
		
		) 
{}
