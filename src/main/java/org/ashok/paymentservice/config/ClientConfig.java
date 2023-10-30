package org.ashok.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfig {
	
	
	@Bean
	public WebClient webClient(ClientProperties clientProperties, WebClient.Builder builder) {
		
		return builder.baseUrl(clientProperties.invoiceServiceUri().toString())
				      .build();
	}

}
