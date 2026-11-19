package com.checkout.payment.gateway.exception;

public class BankUnavailableException extends RuntimeException {
  public BankUnavailableException(Throwable cause) {
    super(cause);
  }
}
