# Teya Ledger

A tiny ledger web API built with Java and Spring Boot. It records deposits and withdrawals, shows the current balance
and lists the transaction history per user. Data is kept in memory, so nothing besides the JDK is needed to run it.

More details:
* [Approach](docs/assignment_approach.md): design decisions, domain model and API design.
* [Assumptions](docs/assignment_assumptions.md): assumptions made where the assignment left room for interpretation.

## Running the app and tests

Requires JDK 25. A separate Maven installation is not required: the Maven Wrapper downloads the version configured in
`.mvn/wrapper/maven-wrapper.properties` on first use (internet access is required).

On macOS/Linux:

```sh
./mvnw test
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The app listens on `http://localhost:8080`.

## API endpoints

All endpoints live under `/ledger/{userId}`, where `userId` is a positive numeric id (anything else returns 400). An
account is created implicitly by the user's first deposit.

| Method | Endpoint                         | Description                                  | Responses                                  |
|--------|----------------------------------|----------------------------------------------|--------------------------------------------|
| POST   | `/ledger/{userId}/transactions`  | Record a `DEPOSIT` or `WITHDRAWAL`           | 201, 400 (invalid body or userId), 404 (withdrawal without account), 422 (insufficient funds) |
| GET    | `/ledger/{userId}/balance`       | Get the current balance                      | 200, 400 (invalid userId), 404 (unknown user) |
| GET    | `/ledger/{userId}/transactions`  | List transactions, most recent first         | 200, 400 (invalid userId), 404 (unknown user) |

Errors are returned as Problem Details.

Request body for `POST`: `{"type": "DEPOSIT" | "WITHDRAWAL", "amount": <positive number>}`

Example:

```sh
curl -X POST http://localhost:8080/ledger/1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "DEPOSIT", "amount": 100.00}'
```

## Example flow

Start the app, then run the following for user `1`. Ids and timestamps will differ in your output. Timestamps are in UTC.

**1. Deposit money (this opens the account)**

```sh
curl -X POST http://localhost:8080/ledger/1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "DEPOSIT", "amount": 100.00}'
```
```json
{"id":"c3e12412-29b0-4e43-9bee-dd70c9ba2abd","timestamp":"2026-10-11T21:33:06.459444Z","type":"DEPOSIT","amount":100.00}
```

**2. Deposit more money**

```sh
curl -X POST http://localhost:8080/ledger/1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "DEPOSIT", "amount": 50.50}'
```
```json
{"id":"b8b5def6-f619-437b-9f0e-d7d520c3dedd","timestamp":"2026-10-11T21:33:06.483514Z","type":"DEPOSIT","amount":50.50}
```

**3. Check balance**

```sh
curl http://localhost:8080/ledger/1/balance
```
```
150.50
```

**4. Withdraw money**

```sh
curl -X POST http://localhost:8080/ledger/1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "WITHDRAWAL", "amount": 30.00}'
```
```json
{"id":"c23af44f-327e-41c8-87d1-b6b76932a353","timestamp":"2026-10-11T21:33:06.514323Z","type":"WITHDRAWAL","amount":30.00}
```

**5. Check balance**

```sh
curl http://localhost:8080/ledger/1/balance
```
```
120.50
```

**6. View transactions (most recent first)**

```sh
curl http://localhost:8080/ledger/1/transactions
```
```json
[
  {"id":"c23af44f-327e-41c8-87d1-b6b76932a353","timestamp":"2026-10-11T21:33:06.514323Z","type":"WITHDRAWAL","amount":30.00},
  {"id":"b8b5def6-f619-437b-9f0e-d7d520c3dedd","timestamp":"2026-10-11T21:33:06.483514Z","type":"DEPOSIT","amount":50.50},
  {"id":"c3e12412-29b0-4e43-9bee-dd70c9ba2abd","timestamp":"2026-10-11T21:33:06.459444Z","type":"DEPOSIT","amount":100.00}
]
```

**7. Withdraw more than the balance (insufficient funds)**

```sh
curl -i -X POST http://localhost:8080/ledger/1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "WITHDRAWAL", "amount": 1000.00}'
```
```
HTTP/1.1 422
```
```json
{"detail":"Insufficient balance for withdrawal","instance":"/ledger/1/transactions","status":422,"title":"Unprocessable Content"}
```

The rejected withdrawal is not recorded, so the balance stays at `120.50`.
