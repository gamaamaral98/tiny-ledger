# Approach

In this file I'll outline the approach taken to implement the assignment, including the design decisions made and the reasoning behind them. 
The goal is to provide a clear understanding of how the solution was structured and why certain choices were made.

## Programming Language and Framework
I chose Java with Spring Boot. Java since I've been using it for the past years and
Spring Boot since I've worked with it before but rarely implemented something from scratch. It's also suitable for REST APIs.

## Functional requirements
* FR1: A user, identified by its unique ID, can check their current balance.
* FR2: A user can check their transaction history.
* FR3: A user can record transactions, which can be either deposits or withdrawals.

## Non-functional requirements
No non-functional requirements were specified in the assignment. I'll keep this a simple implementation, 
focusing on the functional requirements.

## Domain Model

Design decisions based on the assumptions and functional requirements.

### Transaction
Represents a single transaction:
* id (UUID): Unique identifier for the transaction
* timestamp (LocalDateTime): The date and time when the transaction occurred
* type (TransactionType): DEPOSIT or WITHDRAWAL
* amount (BigDecimal): The amount of money involved in the transaction

#### TransactionType
An enum representing the type of transaction:
* DEPOSIT: Represents a deposit transaction
* WITHDRAWAL: Represents a withdrawal transaction

### Account
Represents a user's account:
* balance (BigDecimal): The current balance of the account
* transactions (List<Transaction>): A list of transactions associated with the account

As defined in the assumptions, I'll link the users to their accounts using a Map, where the key is the user id and the value is
the account object. The transactions are stored in a list in chronological order. To present them in reverse chronological order I use
``List.reversed()`` (Java 21), which returns a reversed view without re-sorting.

`Account.getTransactions()` returns an immutable copy (``List.copyOf``) instead of the internal list. This makes every
history read O(n) in the number of transactions, but it protects the history from being modified from outside the
`Account`, which is the only place allowed to append transactions and update the balance. This is a conscious trade-off:
at the scale of this exercise O(n) is irrelevant, and I prefer protecting the integrity of the history.

## Data Persistence
I'm using an in-memory data structure to store user accounts and their associated data. This is a simple implementation
that meets the functional requirements without the need for a database. I'll be using an implementation of the Map interface to store the accounts, 
where the key is the user id and the value is the account object. This way, by using user id I can retrieve the account
with O(1) time complexity (reading its transaction history is O(n), see above).

To mimic a real database, the repository never shares its stored `Account` instances: `findAccount` returns a copy and
`saveAccount` stores a copy. Changes to an `Account` only become visible to other readers once `saveAccount` is called,
which keeps the repository interface honest and storage-agnostic. As a consequence, a rejected transaction (for example
an insufficient-funds withdrawal) never alters the stored account, and a new account is only stored after its first
deposit has been applied successfully.

## API Design
REST API with one endpoint per functional requirement, all under `/ledger/{userId}`, where `userId` identifies the
account owner (authentication is out of scope).
* FR1: GET /ledger/{userId}/balance - Retrieves the current balance for the specified user.
  * Response codes:
    * 200 OK: Successfully retrieved the balance.
    * 404 Not Found: User not found (no account exists for the user).
  * Response body: BigDecimal representing the current balance.
* FR2: GET /ledger/{userId}/transactions - Retrieves the transaction history for the specified user, most recent first.
    * Response codes:
        * 200 OK: Successfully retrieved the transaction history.
        * 404 Not Found: User not found (no account exists for the user).
    * Response body: List of `TransactionResponse` DTOs (id, timestamp, type, amount).
* FR3: POST /ledger/{userId}/transactions - Records a transaction for the specified user.
    * Headers: `uuid` (the transaction id).
    * Response codes:
        * 201 Created: Successfully recorded the transaction.
        * 400 Bad Request - Invalid transaction data:
          * **amount** must be present, non-null, and positive
          * **type** must be present and either DEPOSIT or WITHDRAWAL
        * 404 Not Found: A withdrawal was requested for a user without an account.
        * 422 Unprocessable Entity: Insufficient funds for withdrawal.
    * Accounts are created implicitly: the first DEPOSIT for an unknown user creates the account. A WITHDRAWAL for an
      unknown user is rejected and no account is created.
    * Request body: `TransactionRequest` DTO (type, amount).
    * Response body: `TransactionResponse` DTO (id, timestamp, type, amount).

The domain `Transaction` is never exposed directly. The controller only receives and returns DTOs, and a
`TransactionMapper` converts domain objects to DTOs so that the DTOs don't depend on the domain `Transaction`.

The DTOs do share the domain `TransactionType` enum. I accept this coupling deliberately: the enum is a closed, two-value
set that is part of the business vocabulary, so duplicating it in the DTO layer would only add a mapping step with no
practical benefit for an API of this size.

## Project Structure 
The project is structured in a way that separates concerns. The main components are:
   - **Controller**: Handles incoming HTTP requests and maps them to the appropriate service methods.
   - **Service**: Contains the business logic for handling transactions and balances.
   - **Mapper**: Converts domain objects into DTOs.
   - **Model**: Represents the data structures used in the application.
   - **Repository**: Manages the in-memory data storage.
