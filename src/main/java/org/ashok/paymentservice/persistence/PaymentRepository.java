package org.ashok.paymentservice.persistence;

import org.ashok.paymentservice.domain.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

	Flux<Payment> findAllByUserId(String userId);

}
