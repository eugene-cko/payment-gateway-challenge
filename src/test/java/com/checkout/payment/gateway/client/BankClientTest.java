package com.checkout.payment.gateway.client;

import static com.checkout.payment.gateway.PaymentFixtures.validPaymentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class BankClientTest {

  private static final String BANK_URL = "http://localhost:8080/payments";
  private static final String BANK_REQUEST = """
      {
        "card_number": "2222405343248877",
        "expiry_date": "04/2099",
        "currency": "GBP",
        "amount": 100,
        "cvv": "123"
      }
      """;
  private static final String AUTHORIZED = """
      {"authorized": true, "authorization_code": "auth-code"}
      """;
  private static final String DECLINED = """
      {"authorized": false, "authorization_code": ""}
      """;

  private MockRestServiceServer bank;
  private BankClient bankClient;
  private PaymentRequest request;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    bank = MockRestServiceServer.bindTo(restTemplate).build();
    bankClient = new BankClient(restTemplate, "http://localhost:8080");

    request = validPaymentRequest();
  }

  @Test
  void whenPaymentIsProcessedThenBankReceivesItInBankFormat() {
    bank.expect(requestTo(BANK_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(BANK_REQUEST, true))
        .andRespond(withSuccess(AUTHORIZED, MediaType.APPLICATION_JSON));

    bankClient.processPayment(request);

    bank.verify();
  }

  @Test
  void whenBankAuthorizesThenAuthorizedResponseIsReturned() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(withSuccess(AUTHORIZED, MediaType.APPLICATION_JSON));

    BankResponse response = bankClient.processPayment(request);

    assertThat(response.isAuthorized()).isTrue();
    assertThat(response.getAuthorizationCode()).isEqualTo("auth-code");
  }

  @Test
  void whenBankDeclinesThenDeclinedResponseIsReturned() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(withSuccess(DECLINED, MediaType.APPLICATION_JSON));

    BankResponse response = bankClient.processPayment(request);

    assertThat(response.isAuthorized()).isFalse();
  }

  @Test
  void whenBankReturns503ThenBankUnavailableExceptionIsThrown() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> bankClient.processPayment(request))
        .isInstanceOf(BankUnavailableException.class);
  }

  @Test
  void whenBankTimesOutThenBankUnavailableExceptionIsThrown() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(bankRequest -> {
          throw new SocketTimeoutException();
        });

    assertThatThrownBy(() -> bankClient.processPayment(request))
        .isInstanceOf(BankUnavailableException.class);
  }
}
