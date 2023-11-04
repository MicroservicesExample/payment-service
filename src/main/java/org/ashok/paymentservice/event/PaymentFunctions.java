package org.ashok.paymentservice.event;

import java.util.function.Consumer;

import org.ashok.paymentservice.domain.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;

@Configuration
public class PaymentFunctions {

	private static final Logger logger = LoggerFactory.getLogger(PaymentFunctions.class);
	
	@Bean
	public Consumer<Flux<PaymentNotifiedMessage>> userPaymentNotified(PaymentService service) {
		return messages -> service.consumePaymentNotifiedMessage(messages)
							.doOnNext(payment -> logger.info("The payment {} is notified to the user {}", payment.id(), payment.userId()))
							.subscribe(); // activates the stream, otherwise no data will flow through the stream
				
							
	}
}
