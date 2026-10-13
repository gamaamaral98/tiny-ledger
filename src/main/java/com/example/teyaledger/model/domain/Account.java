package com.example.teyaledger.model.domain;

import com.example.teyaledger.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Account {
    private BigDecimal balance;
    private final List<Transaction> transactions;

    public Account() {
        this.balance = BigDecimal.ZERO;
        this.transactions = new ArrayList<>();
    }

    private Account(BigDecimal balance, List<Transaction> transactions) {
        this.balance = balance;
        this.transactions = new LinkedList<>(transactions);
    }

    public Account copy() {
        return new Account(balance, transactions);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    public void applyTransaction(Transaction transaction) {
        switch (transaction.type()) {
            case DEPOSIT -> balance = balance.add(transaction.amount());
            case WITHDRAWAL -> {
                if (balance.compareTo(transaction.amount()) >= 0) {
                    balance = balance.subtract(transaction.amount());
                } else {
                    throw new InsufficientFundsException("Insufficient balance for withdrawal");
                }
            }
        }

        transactions.addFirst(transaction);
    }
}
