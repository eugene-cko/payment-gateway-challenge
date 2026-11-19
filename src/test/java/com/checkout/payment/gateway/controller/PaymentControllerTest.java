package com.checkout.payment.gateway.controller;


import static com.checkout.payment.gateway.PaymentFixtures.authorizedByBank;
import static com.checkout.payment.gateway.PaymentFixtures.bankUnavailable;
import static com.checkout.payment.gateway.PaymentFixtures.declinedByBank;
import static com.checkout.payment.gateway.PaymentFixtures.validPaymentRequestJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

  private static final String VALID_PAYMENT = validPaymentRequestJson().toString();

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @MockBean
  private BankClient bankClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour("4321");

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card_number_last_four").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error_message").value("Payment not found"));
  }

  @Test
  void whenPaymentIdIsNotAUuidThen400IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payments/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_message").value("Invalid payment id"));
  }

  @Test
  void whenBankAuthorizesPaymentThenAuthorizedStatusIsReturned() throws Exception {
    when(bankClient.processPayment(any())).thenReturn(authorizedByBank());

    mvc.perform(postPayment(VALID_PAYMENT))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.expiry_month").value(4))
        .andExpect(jsonPath("$.expiry_year").value(2099))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.card_number").doesNotExist())
        .andExpect(jsonPath("$.cvv").doesNotExist());
  }

  @Test
  void whenBankDeclinesPaymentThenDeclinedStatusIsReturned() throws Exception {
    when(bankClient.processPayment(any())).thenReturn(declinedByBank());

    mvc.perform(postPayment(VALID_PAYMENT))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
  }

  @Test
  void whenBankIsUnavailableThen503IsReturned() throws Exception {
    when(bankClient.processPayment(any())).thenThrow(bankUnavailable());

    mvc.perform(postPayment(VALID_PAYMENT))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error_message").value("Bank unavailable"));
  }
  @Test
  void whenPaymentIsProcessedThenItCanBeRetrievedById() throws Exception {
    when(bankClient.processPayment(any())).thenReturn(authorizedByBank());

    String body = mvc.perform(postPayment(VALID_PAYMENT))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(body, "$.id");

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"));
  }

  @Test
  void whenCardExpiresThisMonthThenPaymentIsProcessed() throws Exception {
    when(bankClient.processPayment(any())).thenReturn(authorizedByBank());
    YearMonth now = YearMonth.now();
    String content = validPaymentRequestJson()
        .put("expiry_month", now.getMonthValue())
        .put("expiry_year", now.getYear())
        .toString();

    mvc.perform(postPayment(content))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()));
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void whenPaymentIsProcessedThenLogCarriesPaymentIdButNotCardNumber(CapturedOutput output)
      throws Exception {
    when(bankClient.processPayment(any())).thenReturn(authorizedByBank());

    String body = mvc.perform(postPayment(VALID_PAYMENT))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(body, "$.id");

    assertThat(output).contains("Payment " + id + " Authorized for 100 GBP");
    assertThat(output).doesNotContain("2222405343248877");
  }

  private static MockHttpServletRequestBuilder postPayment(String content) {
    return MockMvcRequestBuilders.post("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(content);
  }
}
