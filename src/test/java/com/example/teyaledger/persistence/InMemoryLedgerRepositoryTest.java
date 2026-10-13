package com.example.teyaledger.persistence;

import com.example.teyaledger.model.domain.Account;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLedgerRepositoryTest {

    private final InMemoryLedgerRepository repository = new InMemoryLedgerRepository();

    @Test
    void shouldReturnEmptyWhenAccountDoesNotExist() {
        assertTrue(repository.findAccount(1L).isEmpty());
    }

    @Test
    void shouldReturnCopyOfSavedAccount() {
        Account account = new Account();
        account.applyTransaction(deposit("100.00"));
        repository.saveAccount(1L, account);

        Optional<Account> found = repository.findAccount(1L);

        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("100.00").compareTo(found.get().getBalance()));
        assertEquals(account.getTransactions(), found.get().getTransactions());
        assertNotEquals(found.get(), account);
    }

    @Test
    void shouldOverrideWhenSavingAccount() {
        Account first = new Account();
        first.applyTransaction(deposit("100.00"));
        repository.saveAccount(1L, first);

        Account second = new Account();
        second.applyTransaction(deposit("5.00"));
        repository.saveAccount(1L, second);

        assertEquals(0, new BigDecimal("5.00").compareTo(repository.findAccount(1L).orElseThrow().getBalance()));
    }

    @Test
    void shouldNotAffectAccountByLatterChangesToOriginalThatAreNotSaved() {
        Account account = new Account();
        repository.saveAccount(1L, account);

        account.applyTransaction(deposit("100.00"));

        Account stored = repository.findAccount(1L).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(stored.getBalance()));
        assertTrue(stored.getTransactions().isEmpty());
    }

    private Transaction deposit(String amount) {
        return new Transaction(UUID.randomUUID(), Instant.now(), TransactionType.DEPOSIT, new BigDecimal(amount));
    }
}
