# Payment Gateway

An API that lets merchants take card payments from their shoppers. It validates each payment, forwards it to the acquiring bank, and returns Authorized, Declined or Rejected. Merchants can retrieve any past payment by id.

## Getting started

Requires JDK 17 and Docker.

Local dev run:
```
docker compose up bank_simulator
./gradlew bootRun
```

Docker compose run:
```
docker compose up --build
```

The API is available at http://localhost:8090 and docs are at http://localhost:8090/swagger-ui/index.html

curl requests to try:
```
curl -X POST localhost:8090/payments -H 'Content-Type: application/json' \
-d '{"card_number": "2222405343248877", "expiry_month": 4, "expiry_year": 2099, "currency": "GBP", "amount": 100, "cvv": "123"}'

curl localhost:8090/payments/uuid_from_previous_response
```
Note: With the simulator, the card number's last digit picks the outcome: 
- odd is Authorized, 
- even is Declined,
- 0 makes the bank unavailable.


## API

### `POST /payments`

Processes a card payment: validates it, sends it to the bank and stores the outcome.

**Request**, all fields required:

| Field | Type | Rule |
|---|---|---|
| `card_number` | string | 14-19 digits |
| `expiry_month` | integer | 1-12 |
| `expiry_year` | integer | 2000-2099, month + year not in the past |
| `currency` | enum | `GBP`, `USD` or `EUR` |
| `amount` | integer | Positive, minor units (`100` = £1.00) |
| `cvv` | string | 3-4 digits |

**Responses:**

- `201` Authorized or Declined, with the payment:
  ```json
  {"id": "6f1c2b0e-8d4a-4f6e-9a3b-2c1d0e9f8a7b", "status": "Authorized", "card_number_last_four": "8877", "expiry_month": 4, "expiry_year": 2099, "currency": "GBP", "amount": 100}
  ```
- `400` Rejected. The bank isn't called and nothing is stored:
  ```json
  {"status": "Rejected", "error_message": "Invalid payment request", "error_codes": ["cvv_invalid", "currency_invalid"]}
  ```
  Codes: `<field>_required` or `<field>_invalid` for each field, plus `card_expired` and `request_malformed` for unexpected JSON. All failing fields are reported at once.
- `503` the bank failed, timed out or was unreachable. Nothing is stored: `{"error_message": "Bank unavailable"}`

### `GET /payments/{id}`

Returns a payment by the `id` from `POST /payments`.

**Responses:**

- `200` the payment, same body as above
- `400` the id isn't a UUID: `{"error_message": "Invalid payment id"}`
- `404` no such payment: `{"error_message": "Payment not found"}`

The errors above all carry an `error_message`; `error_codes` appear only on Rejected, where there are fields to fix.

## Testing

```
./gradlew test      # run all tests, except e2e
./gradlew e2eTest   # e2e tests run, need Docker running to start the simulator
./gradlew build     # both
```

| Test | Purpose                               | Web | Service | Repository | Bank client | Bank |
|---|---------------------------------------|---|---|---|---|---|
| `BankClientTest` | Bank request format, failure handling | | | | real | stubbed |
| `PaymentServiceTest` | What gets stored                      | | real | real | mocked | |
| `PaymentsRepositoryTest` | Stores and finds payments             | | | real | | |
| `PaymentValidationTest` | Validation rules                      | real | mocked | | | |
| `PaymentControllerTest` | All API layers, no card data leaks    | real | real | real | mocked | |
| `PaymentE2ETest` | Whole flow over HTTP                  | real | real | real | real | simulator |

## Decisions

- **Sensitive data:** the full card number and CVV are never stored, returned or logged. Only the last four digits are kept.
- **Expiry:** a card expires at the end of its expiry month, so a card expiring this month is still accepted.
- **Declined is 201:** the payment was created and its outcome is in the body, so no 4xx.
- **Amount:** must be positive; `10.5` and `"100"` are rejected, not converted.
- **Bank failures map to 503, nothing stored:** on a bank 5xx, refused connection or no answer. However, the card may still be charged (see Idempotency and Reconciliation)
- **Packaging:** the gateway ships as a Docker image with a health check (`/actuator/health`); compose runs it next to the simulator.

## Next steps

- **Idempotency** for payment requests: a repeated key, even concurrent, returns the original payment instead of a second charge.
- **Reconciliation** for unknown outcomes: a bank call that got no answer is stored as Pending and settled with the bank later.
- **Retries and a circuit breaker** on the bank call: bounded retries on transient failures only, with exponential backoff and jitter, and a circuit breaker that gives a failing bank room to recover while the gateway fails fast.
- **Authentication** on the merchant API: each merchant sees only its own payments (another merchant's payment returns 404, not 403).
- **Persistent storage** for payments: a durable database, so payments survive a restart and the gateway runs as several instances with no single point of failure.
- **Observability** on the payment flow: authorization rate, bank latency and error rate, with dashboards, alerts and a trace id on every request.
- **A CI/CD pipeline**: build and test on every change, load-test before release, then deploy with a smoke test against the bank's sandbox and roll back automatically if it fails.
