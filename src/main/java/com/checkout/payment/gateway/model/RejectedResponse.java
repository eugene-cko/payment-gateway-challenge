package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RejectedResponse {
  private final PaymentStatus status = PaymentStatus.REJECTED;
  @JsonProperty("error_message")
  private final String errorMessage;
  @JsonProperty("error_codes")
  private final List<String> errorCodes;

  public RejectedResponse(String errorMessage, List<String> errorCodes) {
    this.errorMessage = errorMessage;
    this.errorCodes = errorCodes;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public List<String> getErrorCodes() {
    return errorCodes;
  }
}
