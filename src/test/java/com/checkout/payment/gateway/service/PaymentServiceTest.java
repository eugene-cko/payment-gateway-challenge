package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.PaymentFixtures.authorizedByBank;
import static com.checkout.payment.gateway.PaymentFixtures.bankUnavailable;
import static com.checkout.payment.gateway.PaymentFixtures.declinedByBank;
import static com.checkout.payment.gateway.PaymentFixtures.validPaymentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpServerErrorException.BadGateway;

class PaymentServiceTest {

  private BankClient bankClient;
  private PaymentsRepository paymentsRepository;
  private PaymentService service;
  private PaymentRequest request;

  @BeforeEach
  void setUp() {
    bankClient = mock(BankClient.class);
    paymentsRepository = spy(new PaymentsRepository());
    service = new PaymentService(paymentsRepository, bankClient);

    request = validPaymentRequest();
  }

  @Test
  void whenBankAuthorizesThenAuthorizedPaymentIsStored() {
    when(bankClient.processPayment(request)).thenReturn(authorizedByBank());

    PaymentResponse payment = service.processPayment(request);

    assertThat(payment.getId()).isNotNull();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(payment.getCardNumberLastFour()).isEqualTo("8877");
    assertThat(payment.getExpiryMonth()).isEqualTo(4);
    assertThat(payment.getExpiryYear()).isEqualTo(2099);
    assertThat(payment.getCurrency()).isEqualTo("GBP");
    assertThat(payment.getAmount()).isEqualTo(100);
    assertThat(service.getPaymentById(payment.getId())).isSameAs(payment);
  }

  @Test
  void whenBankDeclinesThenDeclinedPaymentIsStored() {
    when(bankClient.processPayment(request)).thenReturn(declinedByBank());

    PaymentResponse payment = service.processPayment(request);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(service.getPaymentById(payment.getId())).isSameAs(payment);
  }
  @Test
  void whenBankIsUnavailableThenNothingIsStored() {
    when(bankClient.processPayment(request)).thenThrow(bankUnavailable());

    assertThatThrownBy(() -> service.processPayment(request))
        .isInstanceOf(BankUnavailableException.class);
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenCardEndsWithLeadingZeroThenLastFourKeepsIt() {
    request.setCardNumber("2222405343240123");
    when(bankClient.processPayment(request)).thenReturn(authorizedByBank());

    PaymentResponse payment = service.processPayment(request);

    assertThat(payment.getCardNumberLastFour()).isEqualTo("0123");
  }

  @Test
  void whenPaymentIdIsUnknownThenPaymentNotFoundExceptionIsThrown() {
    assertThatThrownBy(() -> service.getPaymentById(UUID.randomUUID()))
        .isInstanceOf(PaymentNotFoundException.class);
  }
}
