package org.ashok.paymentservice.domain;

import org.ashok.paymentservice.event.PaymentMessage;
import org.ashok.paymentservice.event.PaymentNotifiedMessage;
import org.ashok.paymentservice.invoice.Invoice;
import org.ashok.paymentservice.invoice.InvoiceClient;
import org.ashok.paymentservice.persistence.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PaymentService {

	private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	private static final String PAYMENT_ACCEPTED_BINDING_NAME = "paymentAccepted-out-0";//paymentAccepted-out-0
	
	
	private final PaymentRepository repository;
	private final InvoiceClient invoiceClient;
	private final StreamBridge streamBridge;
	
	public PaymentService(PaymentRepository repository,
							InvoiceClient invoiceClient,
							StreamBridge streamBridge) {
		
		this.repository = repository;
		this.invoiceClient = invoiceClient;
		this.streamBridge = streamBridge;
	}
	
	public Flux<Payment> getAllPayments(String userId) {
		return repository.findAllByUserId(userId);
	}
	
	public Mono<Payment> getPaymentById(Long id) {
		return repository.findById(id);
	}
	
	@Transactional
	public Mono<Payment> makePayment(Long billRefNum, Integer paymentAmount, String authorizationHeader) {
		
		return invoiceClient.getInvoiceById(billRefNum, authorizationHeader)
							.map(invoice -> buildAcceptedPayment(invoice, paymentAmount))
							.defaultIfEmpty(
											buildRejectedPayment(billRefNum, paymentAmount)
										)
							.flatMap(repository::save)
							.doOnNext(this::publishPaymentAcceptedEvent);
		
	}
	
	private void publishPaymentAcceptedEvent(Payment payment) {
		if(payment.status().equals(PaymentStatus.ACCEPTED)) {
			PaymentMessage message = new PaymentMessage(payment.id(), payment.billRefNumer(), payment.paymentAmount());
			logger.info("Sending payment accepted message with id: {}", message.paymentId());
			
			var result= streamBridge.send(PAYMENT_ACCEPTED_BINDING_NAME, message);
			
			logger.info("Result of sending the payment accepted message with id: {} is {}",message.paymentId(), result );
		}
	}
	
	public Flux<Payment> consumePaymentNotifiedMessage(Flux<PaymentNotifiedMessage> messages) {
		
		return messages
				.flatMap(msg -> repository.findById(msg.paymentId()))
				.map(PaymentService::buildNotifiedPayment)
				.flatMap(repository::save);

	}
	
	public static Payment buildAcceptedPayment(Invoice invoice, Integer paymentAmount) {
		return Payment.of(invoice.id(), invoice.userId(), invoice.amount(), paymentAmount, PaymentStatus.ACCEPTED);
	}
	
	public static Payment buildRejectedPayment(Long billRefNum, Integer paymentAmount) {
		return Payment.of(billRefNum, "not found", paymentAmount, paymentAmount, PaymentStatus.REJECTED);
	}
	
	private static Payment buildNotifiedPayment(Payment existingPayment) {
		return Payment.notifedPayment(existingPayment);
	}
}
