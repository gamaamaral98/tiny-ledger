package com.example.teyaledger.service;

import com.example.teyaledger.exceptions.AccountNotFoundException;
import com.example.teyaledger.model.domain.Account;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;
import com.example.teyaledger.persistence.LedgerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    @Override
    public Transaction makeTransaction(UUID uuid, Long userId, TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction(uuid, LocalDateTime.now(), type, amount);

        if (type.equals(TransactionType.DEPOSIT)) return makeDeposit(userId, transaction);
        else return makeWithdrawal(userId, transaction);
    }

    private Transaction makeDeposit(Long userId, Transaction transaction) {
        Account account = ledgerRepository.findAccount(userId).orElse(new Account());
        account.applyTransaction(transaction);
        ledgerRepository.saveAccount(userId, account);
        return transaction;
    }

    private Transaction makeWithdrawal(Long userId, Transaction transaction) {
        Account account = findAccount(userId);
        account.applyTransaction(transaction);
        ledgerRepository.saveAccount(userId, account);
        return transaction;
    }

    private Account findAccount(Long userId) {
        return ledgerRepository.findAccount(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for userId: " + userId));
    }
}
