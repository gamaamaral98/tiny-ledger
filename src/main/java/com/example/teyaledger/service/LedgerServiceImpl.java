package com.example.teyaledger.service;

import com.example.teyaledger.exceptions.AccountNotFoundException;
import com.example.teyaledger.model.domain.Account;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;
import com.example.teyaledger.persistence.LedgerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;

    public LedgerServiceImpl(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        Account account = findAccount(userId);
        return account.getBalance();
    }

    @Override
    public List<Transaction> getTransactions(Long userId) {
        Account account = findAccount(userId);
        List<Transaction> transactions = account.getTransactions();
        return transactions.reversed();
    }

    private Account findAccount(Long userId) {
        return ledgerRepository.findAccount(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for userId: " + userId));
    }

    @Override
    public Transaction makeTransaction(Long userId, TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction(UUID.randomUUID(), Instant.now(), type, amount);

        Account account = ledgerRepository.findAccount(userId)
                .orElseGet(() -> newAccountFor(userId, type));
        account.applyTransaction(transaction);
        ledgerRepository.saveAccount(userId, account);

        return transaction;
    }

    private Account newAccountFor(Long userId, TransactionType type) {
        if (type == TransactionType.WITHDRAWAL) {
            throw new AccountNotFoundException("Account not found for userId: " + userId);
        }
        return new Account();
    }
}
