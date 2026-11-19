package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
  @JsonProperty("error_message")
  private final String errorMessage;

  public ErrorResponse(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "errorMessage='" + errorMessage + '\'' +
        '}';
  }
}
