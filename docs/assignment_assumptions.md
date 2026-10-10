# Assumptions

In this file I'll document any assumptions made during the analysis and implementation phase of the assignment. 
The goal is to provide clarity on the decisions made and the reasoning behind them.

List of Assumptions:
1. **Multiple Users**: Although the assignment does not specifically mention an estimation for the number of users, I'm 
assuming that it's possible to tell users apart by "id" and perform transactions on different accounts.
2. **1-to-1 relationship between user and account**: I'm assuming that each user has exactly one account, and each 
account belongs to exactly one user. This simplifies the design and implementation of the system.
3. **No transactions between users**: It's specified that the transactions available are deposits and withdrawals, therefore
I'm not implementing any transactions between users.
4. **No negative balances**: I'm assuming that the system should not allow users to withdraw more money than they have
in their account to keep simplicity.
5. **Map as data repository**: Since it's suggested that I use in-memory data structures I'm assuming that a map is a 
suitable data structure to store user accounts and their balances, as well as transaction history. Each user 
will have a unique identifier that maps to their account information.
6. **Transaction Types**: I'm assuming that the only transaction types are deposits and withdrawals, as specified in the assignment.
7. **Transaction history**: I'm assuming that the transaction history can be stored within a list associated with each 
user account in the map. The transactions will be presented in reverse chronological order to simulate a real-world scenario which is more read-friendly.
8. **No concurrency handling**: Given the simplicity of the assignment and the use of in-memory data structures, 
I'm assuming that there will be no concurrent access to the ledger, and thus no need for locks or other concurrency control mechanisms.
9. **Failed transactions**: Failed transactions will be dropped for the sake of simplicity. In a real-world scenario
these would integrate with a logging system or some kind of persistent storage for audit trail.
10. **Implicit account creation**: There is no endpoint to create accounts. An account is created the first time a
user makes a deposit. A withdrawal for a user without an account fails with 404 (Not Found) and does not create one,
and balance or history requests for an unknown user also return 404.
