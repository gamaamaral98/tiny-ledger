package com.example.teyaledger.service;

import com.example.teyaledger.exceptions.AccountNotFoundException;
import com.example.teyaledger.exceptions.InsufficientFundsException;
import com.example.teyaledger.model.domain.Account;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;
import com.example.teyaledger.persistence.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private LedgerRepository ledgerRepository;
    @InjectMocks
    private LedgerServiceImpl ledgerService;

    private Transaction transaction(TransactionType type, String amount) {
        return new Transaction(UUID.randomUUID(), Instant.now(), type, new BigDecimal(amount));
    }

    private Account accountWithDeposit(String amount) {
        Account account = new Account();
        account.applyTransaction(transaction(TransactionType.DEPOSIT, amount));
        return account;
    }

    @Test
    void shouldReturnBalanceWhenAccountExists() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.of(accountWithDeposit("100.00")));

        BigDecimal balance = ledgerService.getBalance(USER_ID);

        assertEquals(0, new BigDecimal("100.00").compareTo(balance));
    }

    @Test
    void shouldThrowAccountNotFoundWhenGettingBalanceOfUnknownAccount() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> ledgerService.getBalance(USER_ID));
    }

    @Test
    void shouldReturnTransactionsNewestFirstWhenAccountExists() {
        Account account = new Account();
        Transaction first = transaction(TransactionType.DEPOSIT, "100.00");
        Transaction second = transaction(TransactionType.WITHDRAWAL, "30.00");
        Transaction third = transaction(TransactionType.DEPOSIT, "5.00");
        account.applyTransaction(first);
        account.applyTransaction(second);
        account.applyTransaction(third);
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.of(account));

        List<Transaction> transactions = ledgerService.getTransactions(USER_ID);

        assertEquals(List.of(third, second, first), transactions);
    }

    @Test
    void shouldReturnEmptyTransactionsWhenAccountHasNoTransactions() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.of(new Account()));

        assertTrue(ledgerService.getTransactions(USER_ID).isEmpty());
    }

    @Test
    void shouldThrowAccountNotFoundWhenGettingTransactionsOfUnknownAccount() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> ledgerService.getTransactions(USER_ID));
    }

    @Test
    void shouldCreateAccountAndSaveWhenDepositingToUnknownAccount() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.empty());

        Transaction result = ledgerService.makeTransaction(USER_ID, TransactionType.DEPOSIT, new BigDecimal("50.00"));

        assertNotNull(result.id());
        assertEquals(TransactionType.DEPOSIT, result.type());
        assertEquals(0, new BigDecimal("50.00").compareTo(result.amount()));
        assertNotNull(result.timestamp());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(ledgerRepository).saveAccount(eq(USER_ID), captor.capture());
        assertEquals(List.of(result), captor.getValue().getTransactions());
    }

    @Test
    void shouldApplyTransactionAndSaveWhenDepositingToExistingAccount() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.of(accountWithDeposit("100.00")));

        Transaction result = ledgerService.makeTransaction(USER_ID, TransactionType.DEPOSIT, new BigDecimal("25.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(ledgerRepository).saveAccount(eq(USER_ID), captor.capture());
        assertEquals(result, captor.getValue().getTransactions().getFirst());
    }

    @Test
    void shouldSaveWhenWithdrawingWithSufficientFunds() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.of(accountWithDeposit("100.00")));

        Transaction result = ledgerService.makeTransaction(USER_ID, TransactionType.WITHDRAWAL, new BigDecimal("40.00"));

        assertNotNull(result.id());
        assertEquals(TransactionType.WITHDRAWAL, result.type());

        verify(ledgerRepository).saveAccount(eq(USER_ID), any(Account.class));
    }

    @Test
    void shouldGenerateDistinctIdsForEachTransaction() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.empty());

        Transaction first = ledgerService.makeTransaction(USER_ID, TransactionType.DEPOSIT, new BigDecimal("10.00"));
        Transaction second = ledgerService.makeTransaction(USER_ID, TransactionType.DEPOSIT, new BigDecimal("10.00"));

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void shouldThrowInsufficientFundsAndNotSaveWhenWithdrawingMoreThanBalance() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.of(accountWithDeposit("10.00")));

        assertThrows(InsufficientFundsException.class,
                () -> ledgerService.makeTransaction(USER_ID, TransactionType.WITHDRAWAL, new BigDecimal("10.01")));

        verify(ledgerRepository, never()).saveAccount(any(), any());
    }

    @Test
    void shouldThrowAccountNotFoundAndNotSaveWhenWithdrawingFromUnknownAccount() {
        when(ledgerRepository.findAccount(USER_ID)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> ledgerService.makeTransaction(USER_ID, TransactionType.WITHDRAWAL, new BigDecimal("1.00")));

        verify(ledgerRepository, never()).saveAccount(any(), any());
    }
}
