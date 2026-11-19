package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting payment {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
  }

  public PaymentResponse processPayment(PaymentRequest paymentRequest) {
    BankResponse bankResponse = bankClient.processPayment(paymentRequest);
    PaymentResponse payment = toPaymentResponse(paymentRequest, bankResponse);
    paymentsRepository.add(payment);
    LOG.info("Payment {} {} for {} {}", payment.getId(), payment.getStatus().getName(),
        payment.getAmount(), payment.getCurrency());
    return payment;
  }

  private PaymentResponse toPaymentResponse(PaymentRequest request,
      BankResponse bankResponse) {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setStatus(bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED);
    payment.setCardNumberLastFour(request.getCardNumberLastFour());
    payment.setExpiryMonth(request.getExpiryMonth());
    payment.setExpiryYear(request.getExpiryYear());
    payment.setCurrency(request.getCurrency());
    payment.setAmount(request.getAmount());
    return payment;
  }
}
