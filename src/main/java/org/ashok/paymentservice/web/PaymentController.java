package org.ashok.paymentservice.web;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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

	/**
	 * Admin user can view any payment, while the normal user can view his/her own payment only.
	 * @param userId
	 * @param jwt
	 * @return
	 */
	@GetMapping
	public Flux<Payment> getAllPayments(String userId, @AuthenticationPrincipal Jwt jwt){
		
		if(isValidUser(userId, jwt)) {
			return service.getAllPayments(userId);
		}
		return Flux.empty();
	}
	
	/**
	 * Returns true only if the jwt user is admin or the jwt user matches with userId
	 * @param userId
	 * @param jwt
	 * @return
	 */
	private boolean isValidUser(String userId, Jwt jwt) {
		
		if(jwt.getSubject().equals(userId)) {
			return true;
		}
		else {
			List<String> roles = jwt.getClaimAsStringList("roles");
			System.out.println("Roles - " + roles);
			if(roles != null && roles.contains("ADMIN")) {
				return true;
			}
		}
		
		return false;				
	}

	@GetMapping("{id}")
	public Mono<Payment> getPayment(@PathVariable Long id){
		return service.getPaymentById(id);
	}
	
	@PostMapping
	public Mono<Payment> makePayment(@RequestBody @Valid PaymentRequest request, @RequestHeader("Authorization") String authorizationHeader) {
	
		return service.makePayment(request.billRefNumber(), request.amount(), authorizationHeader);
	}
}
