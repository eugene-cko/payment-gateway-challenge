package com.checkout.payment.gateway;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

public final class PaymentFixtures {

  private PaymentFixtures() {
  }

  public static PaymentRequest validPaymentRequest() {
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2099);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

  public static ObjectNode validPaymentRequestJson() {
    return JsonNodeFactory.instance.objectNode()
        .put("card_number", "2222405343248877")
        .put("expiry_month", 4)
        .put("expiry_year", 2099)
        .put("currency", "GBP")
        .put("amount", 100)
        .put("cvv", "123");
  }

  public static BankResponse authorizedByBank() {
    BankResponse response = new BankResponse();
    response.setAuthorized(true);
    response.setAuthorizationCode("auth-code");
    return response;
  }

  public static BankUnavailableException bankUnavailable() {
    return new BankUnavailableException(
        new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
  }

  public static BankResponse declinedByBank() {
    BankResponse response = new BankResponse();
    response.setAuthorized(false);
    return response;
  }
}
