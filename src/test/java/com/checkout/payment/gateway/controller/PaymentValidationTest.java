package com.checkout.payment.gateway.controller;

import static com.checkout.payment.gateway.PaymentFixtures.validPaymentRequestJson;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(PaymentController.class)
class PaymentValidationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired
  private MockMvc mvc;
  @MockBean
  private PaymentService paymentService;

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', textBlock = """
      null                   | card_number_required
      "2222405343248"        | card_number_invalid
      "22224053432488771234" | card_number_invalid
      "222240534324887X"     | card_number_invalid
      """)
  void rejectsInvalidCardNumber(String value, String errorCode) throws Exception {
    assertFieldRejected("card_number", value, errorCode);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', textBlock = """
      null | expiry_month_required
      0    | expiry_month_invalid
      13   | expiry_month_invalid
      """)
  void rejectsInvalidExpiryMonth(String value, String errorCode) throws Exception {
    assertFieldRejected("expiry_month", value, errorCode);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', textBlock = """
      null | expiry_year_required
      2020 | card_expired
      2100 | expiry_year_invalid
      """)
  void rejectsInvalidExpiryYear(String value, String errorCode) throws Exception {
    assertFieldRejected("expiry_year", value, errorCode);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', textBlock = """
      null  | currency_required
      "JPY" | currency_invalid
      """)
  void rejectsInvalidCurrency(String value, String errorCode) throws Exception {
    assertFieldRejected("currency", value, errorCode);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', textBlock = """
      null  | amount_required
      0     | amount_invalid
      10.5  | request_malformed
      "abc" | request_malformed
      "100" | request_malformed
      """)
  void rejectsInvalidAmount(String value, String errorCode) throws Exception {
    assertFieldRejected("amount", value, errorCode);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', textBlock = """
      null    | cvv_required
      "12"    | cvv_invalid
      "12345" | cvv_invalid
      "12a"   | cvv_invalid
      """)
  void rejectsInvalidCvv(String value, String errorCode) throws Exception {
    assertFieldRejected("cvv", value, errorCode);
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    assertRejected("{\"card_number\": \"2222405343248877\",", "request_malformed");
  }

  @Test
  void reportsEveryInvalidField() throws Exception {
    ObjectNode request = validPaymentRequestJson().put("cvv", "12").put("currency", "JPY");
    post(request.toString())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_codes")
            .value(containsInAnyOrder("cvv_invalid", "currency_invalid")));
    verifyNoInteractions(paymentService);
  }

  @Test
  void passesValidRequestToService() throws Exception {
    post(validPaymentRequestJson().toString())
        .andExpect(status().isCreated());
    verify(paymentService).processPayment(any());
  }

  @ParameterizedTest(name = "{0} = {1}")
  @CsvSource(delimiter = '|', textBlock = """
      card_number  | "22224053432488"
      card_number  | "2222405343248877123"
      cvv          | "1234"
      amount       | 1
      expiry_month | 1
      expiry_month | 12
      """)
  void acceptsBoundaryValues(String field, String json) throws Exception {
    ObjectNode request = validPaymentRequestJson();
    request.set(field, MAPPER.readTree(json));
    post(request.toString())
        .andExpect(status().isCreated());
    verify(paymentService).processPayment(any());
  }

  @Test
  void rejectsCardExpiredLastMonth() throws Exception {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    ObjectNode request = validPaymentRequestJson()
        .put("expiry_month", lastMonth.getMonthValue())
        .put("expiry_year", lastMonth.getYear());
    assertRejected(request.toString(), "card_expired");
  }

  private void assertFieldRejected(String field, String json, String errorCode)
      throws Exception {
    ObjectNode request = validPaymentRequestJson();
    request.set(field, MAPPER.readTree(json));
    assertRejected(request.toString(), errorCode);
  }

  private void assertRejected(String content, String errorCode) throws Exception {
    post(content)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.error_message").isNotEmpty())
        .andExpect(jsonPath("$.error_codes").value(contains(errorCode)));
    verifyNoInteractions(paymentService);
  }

  private ResultActions post(String content) throws Exception {
    return mvc.perform(MockMvcRequestBuilders.post("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(content));
  }
}
