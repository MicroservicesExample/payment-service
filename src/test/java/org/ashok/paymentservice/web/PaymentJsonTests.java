package org.ashok.paymentservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.ashok.paymentservice.domain.Payment;
import org.ashok.paymentservice.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

/**
 * Using the @JsonTest annotation, you can test JSON serialization and
	deserialization for your domain objects
 * @author Ashok Mane
 *
 */

@JsonTest
class PaymentJsonTests {

	@Autowired
	JacksonTester<Payment> json;
	
	
	
	@Test
	void testSerialize() throws IOException {
		Payment payment = Payment.of(123L, "test@gmail.com", 200, 100, PaymentStatus.ACCEPTED);
		
		var jsonContent = json.write(payment);
		
		assertThat(jsonContent).extractingJsonPathNumberValue("@.id")
		.isNull();
		assertThat(jsonContent).extractingJsonPathNumberValue("@.billRefNumer")
			.isEqualTo(payment.billRefNumer().intValue());
		assertThat(jsonContent).extractingJsonPathStringValue("@.userId")
			.isEqualTo(payment.userId());
		assertThat(jsonContent).extractingJsonPathNumberValue("@.billAmount")
			.isEqualTo(payment.billAmount());
		assertThat(jsonContent).extractingJsonPathNumberValue("@.paymentAmount")
			.isEqualTo(payment.paymentAmount());
		assertThat(jsonContent).extractingJsonPathStringValue("@.status")
			.isEqualTo(payment.status().toString());
		assertThat(jsonContent).extractingJsonPathNumberValue("@.version")
		.isEqualTo(0);
		assertThat(jsonContent).extractingJsonPathValue("@.created_date")
				.isNull();
		assertThat(jsonContent).extractingJsonPathValue("@.last_modified_date")
				.isNull();
		
	}
	
	@Test
	void testDeserialize() throws IOException {
		var jsonContent = 
			"""
			{
			  "id": 1,
			  "billRefNumer": 45,
			  "userId": "test@gamil.com",
			  "billAmount": 6500,
			  "paymentAmount": 500,
			  "status": "ACCEPTED",
			  "version": 1,
			  "createdDate": "2023-10-30T12:33:39",
			  "lastModifiedDate": "2023-10-30T12:33:39"
			}
			""";
		
		LocalDateTime ldt = LocalDateTime.parse("2023-10-30T12:33:39", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
		
		
		
		assertThat(json.parse(jsonContent))
		.usingRecursiveComparison()
		.isEqualTo(new Payment(1L, 45L, "test@gamil.com", 6500, 500, PaymentStatus.ACCEPTED, 1, ldt, ldt));
	}

}
