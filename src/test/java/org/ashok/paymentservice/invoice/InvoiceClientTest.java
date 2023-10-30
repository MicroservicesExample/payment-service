package org.ashok.paymentservice.invoice;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class InvoiceClientTest {

	private MockWebServer mockWebServer;
	private InvoiceClient invoiceClient;
	
	
	@BeforeEach
	void setup() throws IOException {
		this.mockWebServer = new MockWebServer();
		this.mockWebServer.start();
		
		var webClient = WebClient.builder()
						.baseUrl(mockWebServer.url("/").uri().toString())
						.build();
		this.invoiceClient = new InvoiceClient(webClient);
	}
	
	@AfterEach
	void clean() throws IOException {
		this.mockWebServer.shutdown();
	}
	
	@Test
	void whenInvoiceExistsReturnInvoice() {
		var invoiceId = 45L;
		
		var mockResponse = new MockResponse()
							.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
							.setBody("""
									{
										"id": %s,
										"userId": "test@gmail.com",
										"amount": 6500,
										"dueDate": "2023-11-30"
									}
									""".formatted(invoiceId));
		mockWebServer.enqueue(mockResponse);
		Mono<Invoice> invoice = invoiceClient.getInvoiceById(invoiceId);
		
		StepVerifier.create(invoice)
			.expectNextMatches(
								in -> in.id().equals(invoiceId))
			.verifyComplete();	//verifies that the reactive stream completed successfully								
			
	}

}
