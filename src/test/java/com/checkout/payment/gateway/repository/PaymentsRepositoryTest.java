package com.checkout.payment.gateway.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentsRepositoryTest {

  private final PaymentsRepository repository = new PaymentsRepository();

  @Test
  void whenPaymentIsAddedThenItCanBeFoundById() {
    PaymentResponse payment = payment();

    repository.add(payment);

    assertThat(repository.get(payment.getId())).containsSame(payment);
  }

  @Test
  void whenIdIsUnknownThenNothingIsFound() {
    assertThat(repository.get(UUID.randomUUID())).isEmpty();
  }

  @Test
  void whenTwoPaymentsAreAddedThenEachIsFoundByItsOwnId() {
    PaymentResponse first = payment();
    PaymentResponse second = payment();

    repository.add(first);
    repository.add(second);

    assertThat(repository.get(first.getId())).containsSame(first);
    assertThat(repository.get(second.getId())).containsSame(second);
  }

  private static PaymentResponse payment() {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    return payment;
  }
}
