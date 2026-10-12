package com.example.teyaledger.model.domain;

import com.example.teyaledger.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Transaction transaction(TransactionType type, String amount) {
        return new Transaction(UUID.randomUUID(), Instant.now(), type, new BigDecimal(amount));
    }

    @Test
    void shouldHaveZeroBalanceAndNoTransactionsOnNewAccount() {
        Account account = new Account();

        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        assertTrue(account.getTransactions().isEmpty());
    }

    @Test
    void shouldIncreaseBalanceAndRecordTransaction() {
        Account account = new Account();
        Transaction deposit = transaction(TransactionType.DEPOSIT, "100.00");

        account.applyTransaction(deposit);

        assertEquals(0, new BigDecimal("100.00").compareTo(account.getBalance()));
        assertEquals(1, account.getTransactions().size());
        assertSame(deposit, account.getTransactions().getFirst());
    }

    @Test
    void shouldDecreaseBalanceAndRecordTransaction() {
        Account account = new Account();
        account.applyTransaction(transaction(TransactionType.DEPOSIT, "100.00"));

        account.applyTransaction(transaction(TransactionType.WITHDRAWAL, "40.00"));

        assertEquals(0, new BigDecimal("60.00").compareTo(account.getBalance()));
        assertEquals(2, account.getTransactions().size());
    }

    @Test
    void shouldWithdrawEntireBalance() {
        Account account = new Account();
        account.applyTransaction(transaction(TransactionType.DEPOSIT, "50.00"));

        account.applyTransaction(transaction(TransactionType.WITHDRAWAL, "50.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    void shouldThrowWhenInsufficientBalanceForWithdrawal() {
        Account account = new Account();
        account.applyTransaction(transaction(TransactionType.DEPOSIT, "50.00"));

        assertThrows(InsufficientFundsException.class,
                () -> account.applyTransaction(transaction(TransactionType.WITHDRAWAL, "50.01")));

        assertEquals(0, new BigDecimal("50.00").compareTo(account.getBalance()));
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void shouldBeIndependentFromCopy() {
        Account original = new Account();
        original.applyTransaction(transaction(TransactionType.DEPOSIT, "100.00"));

        Account copy = original.copy();
        copy.applyTransaction(transaction(TransactionType.WITHDRAWAL, "30.00"));

        assertEquals(0, new BigDecimal("100.00").compareTo(original.getBalance()));
        assertEquals(1, original.getTransactions().size());
        assertEquals(0, new BigDecimal("70.00").compareTo(copy.getBalance()));
        assertEquals(2, copy.getTransactions().size());
    }
}
