package org.ashok.paymentservice.web;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payments")
public class PaymentController {
	
	private final PaymentService service;
	
	public PaymentController(PaymentService service) {
		this.service = service;
	}

	@GetMapping
	public Flux<Payment> getAllPayments(String userId){
		return service.getAllPayments(userId);
	}
	
	@GetMapping("{id}")
	public Mono<Payment> getPayment(@PathVariable Long id){
		return service.getPaymentById(id);
	}
	
	@PostMapping
	public Mono<Payment> makePayment(@RequestBody @Valid PaymentRequest request) {
	
		return service.makePayment(request.billRefNumber(), request.amount());
	}
}
