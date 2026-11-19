package com.checkout.payment.gateway;

import static com.checkout.payment.gateway.PaymentFixtures.validPaymentRequestJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Tag("e2e")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentE2ETest {

  private static final String IMPOSTERS_PATH = "imposters/bank_simulator.ejs";

  @Container
  private static final GenericContainer<?> BANK =
      new GenericContainer<>("bbyars/mountebank:2.8.1")
          .withCopyFileToContainer(MountableFile.forHostPath(IMPOSTERS_PATH), "/" + IMPOSTERS_PATH)
          .withCommand("--configfile", "/" + IMPOSTERS_PATH, "--allowInjection")
          .withExposedPorts(8080);

  @DynamicPropertySource
  static void bankUrl(DynamicPropertyRegistry registry) {
    registry.add("bank.url", () -> "http://" + BANK.getHost() + ":" + BANK.getMappedPort(8080));
  }

  @Autowired
  private TestRestTemplate rest;

  @Test
  void whenCardEndsInOddDigitThenPaymentIsAuthorizedAndCanBeRetrieved() {
    ResponseEntity<JsonNode> created = postPayment("2222405343248877");

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().get("status").asText()).isEqualTo("Authorized");

    String id = created.getBody().get("id").asText();
    ResponseEntity<JsonNode> fetched = rest.getForEntity("/payments/" + id, JsonNode.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody().get("status").asText()).isEqualTo("Authorized");
    assertThat(fetched.getBody().get("card_number_last_four").asText()).isEqualTo("8877");
  }

  @Test
  void whenCardEndsInEvenDigitThenPaymentIsDeclined() {
    ResponseEntity<JsonNode> created = postPayment("2222405343248878");

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().get("status").asText()).isEqualTo("Declined");
  }

  @Test
  void whenCardEndsInZeroThenBankUnavailableIsReturned() {
    ResponseEntity<JsonNode> response = postPayment("2222405343248870");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  private ResponseEntity<JsonNode> postPayment(String cardNumber) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String body = validPaymentRequestJson().put("card_number", cardNumber).toString();
    return rest.postForEntity("/payments", new HttpEntity<>(body, headers), JsonNode.class);
  }
}
