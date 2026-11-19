package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class BankClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankClient.class);

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public BankClient(RestTemplate restTemplate, @Value("${bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  public BankResponse processPayment(PaymentRequest paymentRequest) {
    try {
      String url = bankUrl + "/payments";
      BankPaymentRequest request = toBankRequest(paymentRequest);
      return restTemplate.postForEntity(url, request, BankResponse.class).getBody();
    } catch (HttpServerErrorException | ResourceAccessException ex) {
      LOG.error("Bank request failed: {}", ex.getMessage());
      throw new BankUnavailableException(ex);
    }
  }

  private BankPaymentRequest toBankRequest(PaymentRequest request) {
    String expiryDate = String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear());
    return new BankPaymentRequest(request.getCardNumber(), expiryDate, request.getCurrency(),
        request.getAmount(), request.getCvv());
  }
}
