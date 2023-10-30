package org.ashok.paymentservice.domain;

import org.ashok.paymentservice.invoice.Invoice;
import org.ashok.paymentservice.invoice.InvoiceClient;
import org.ashok.paymentservice.persistence.PaymentRepository;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PaymentService {

	private final PaymentRepository repository;
	private final InvoiceClient invoiceClient;
	
	public PaymentService(PaymentRepository repository,
							InvoiceClient invoiceClient) {
		
		this.repository = repository;
		this.invoiceClient = invoiceClient;
	}
	
	public Flux<Payment> getAllPayments(String userId) {
		return repository.findAllByUserId(userId);
	}
	
	public Mono<Payment> getPaymentById(Long id) {
		return repository.findById(id);
	}
	
	public Mono<Payment> makePayment(Long billRefNum, Integer paymentAmount) {
		
		return invoiceClient.getInvoiceById(billRefNum)
							.map(invoice -> buildAcceptedPayment(invoice, paymentAmount))
							.defaultIfEmpty(
											buildRejectedPayment(billRefNum, paymentAmount)
										)
							.flatMap(repository::save);

		
	}
	
	public static Payment buildAcceptedPayment(Invoice invoice, Integer paymentAmount) {
		return Payment.of(invoice.id(), invoice.userId(), invoice.amount(), paymentAmount, PaymentStatus.ACCEPTED);
	}
	
	public static Payment buildRejectedPayment(Long billRefNum, Integer paymentAmount) {
		return Payment.of(billRefNum, "not found", paymentAmount, paymentAmount, PaymentStatus.REJECTED);
	}
	
}
