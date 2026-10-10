package com.example.teyaledger.persistence;

import com.example.teyaledger.model.domain.Account;

import java.util.Optional;

public interface LedgerRepository {
    Optional<Account> findAccount(Long userId);
    void saveAccount(Long userId, Account account);
}
