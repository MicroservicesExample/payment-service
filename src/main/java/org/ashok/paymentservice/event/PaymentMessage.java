package org.ashok.paymentservice.event;

public record PaymentMessage(Long paymentId, Long billRefNumber, Integer amount) {

}
