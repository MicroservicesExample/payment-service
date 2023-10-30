package org.ashok.paymentservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class PaymentRequestJsonTests {

	@Autowired
	JacksonTester<PaymentRequest> json;
	
	
	
	
	@Test
	void testSerialize() throws IOException {
		PaymentRequest request = new PaymentRequest(1234L, 200);
		
		var jsonContent = json.write(request);
		
		assertThat(jsonContent).extractingJsonPathNumberValue("@.billRefNumber")
			.isEqualTo(request.billRefNumber().intValue());
		assertThat(jsonContent).extractingJsonPathNumberValue("@.amount")
			.isEqualTo(request.amount());
	}
	
	@Test
	void testDeserialize() throws IOException {
		var jsonContent = """
				{
				  "billRefNumber":45,
				  "amount":500
				}
				""";
		
		assertThat(json.parse(jsonContent))
		.usingRecursiveComparison()
		.isEqualTo(new PaymentRequest(45L, 500));
	}

}
