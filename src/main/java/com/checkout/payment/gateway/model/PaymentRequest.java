package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.YearMonth;

public class PaymentRequest implements Serializable {

  @NotNull(message = "card_number_required")
  @Pattern(regexp = "\\d{14,19}", message = "card_number_invalid")
  @JsonProperty("card_number")
  private String cardNumber;
  @NotNull(message = "expiry_month_required")
  @Min(value = 1, message = "expiry_month_invalid")
  @Max(value = 12, message = "expiry_month_invalid")
  @JsonProperty("expiry_month")
  private Integer expiryMonth;
  @NotNull(message = "expiry_year_required")
  @Min(value = 2000, message = "expiry_year_invalid")
  @Max(value = 2099, message = "expiry_year_invalid")
  @JsonProperty("expiry_year")
  private Integer expiryYear;
  @NotNull(message = "currency_required")
  @Pattern(regexp = "GBP|USD|EUR", message = "currency_invalid")
  private String currency;
  @NotNull(message = "amount_required")
  @Positive(message = "amount_invalid")
  private Integer amount;
  @NotNull(message = "cvv_required")
  @Pattern(regexp = "\\d{3,4}", message = "cvv_invalid")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  @JsonIgnore
  public String getCardNumberLastFour() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return null;
    }
    return cardNumber.substring(cardNumber.length() - 4);
  }

  @JsonIgnore
  @FutureOrPresent(message = "card_expired")
  public YearMonth getExpiry() {
    if (expiryMonth == null || expiryYear == null || expiryMonth < 1 || expiryMonth > 12) {
      return null;
    }
    return YearMonth.of(expiryYear, expiryMonth);
  }

  @Override
  public String toString() {
    return "PaymentRequest{" +
        "cardNumberLastFour=" + getCardNumberLastFour() +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
