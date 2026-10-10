package com.example.teyaledger.service;

import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerService {
    BigDecimal getBalance(Long userId);
    List<Transaction> getTransactions(Long userId);
    Transaction makeTransaction(UUID uuid, Long userId, TransactionType type, BigDecimal amount);
}
