package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.RejectedResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(PaymentNotFoundException ex) {
    LOG.warn(ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse("Payment not found"), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleException(MethodArgumentTypeMismatchException ex) {
    LOG.warn("Invalid payment id: {}", ex.getValue());
    return new ResponseEntity<>(new ErrorResponse("Invalid payment id"), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BankUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleException(BankUnavailableException ex) {
    return new ResponseEntity<>(new ErrorResponse("Bank unavailable"),
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RejectedResponse> handleException(MethodArgumentNotValidException ex) {
    List<String> errorCodes = ex.getBindingResult().getAllErrors().stream()
        .map(ObjectError::getDefaultMessage)
        .toList();
    LOG.warn("Payment rejected: {}", errorCodes);
    return new ResponseEntity<>(new RejectedResponse("Invalid payment request", errorCodes),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RejectedResponse> handleException(HttpMessageNotReadableException ex) {
    LOG.warn("Payment rejected: [request_malformed]");
    return new ResponseEntity<>(
        new RejectedResponse("Invalid payment request", List.of("request_malformed")),
        HttpStatus.BAD_REQUEST);
  }
}
