package org.ashok.paymentservice.invoice;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class InvoiceClient {

	private static final String INVOICE_ROOT_API = "/invoices/";
	
	private final WebClient webClient;
	
	public InvoiceClient(WebClient webClient) {
		this.webClient = webClient;
	}
	
	public Mono<Invoice> getInvoiceById(Long id){
		return webClient
				   .get()
				   .uri(INVOICE_ROOT_API + id)
				   .retrieve()
				   .bodyToMono(Invoice.class)
				   .timeout(Duration.ofSeconds(3), Mono.empty()) //request times out in 3s and returns empty mono as fallback which will reject the payment
				   .onErrorResume(WebClientResponseException.NotFound.class,
						   			exception -> Mono.empty()) // if 404 error then no retries and return empty mono as fallback
				   .retryWhen(
						   Retry.backoff(3, Duration.ofMillis(100)) // 3 re tries with 100ms initial backoff
						   )
					.onErrorResume(Exception.class,
							exception -> Mono.empty()); // if error occurs even after 3 re tries, return empty mono as fallback
	}
}
