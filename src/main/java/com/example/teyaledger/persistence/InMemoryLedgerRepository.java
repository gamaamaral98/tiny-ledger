package com.example.teyaledger.persistence;

import com.example.teyaledger.model.domain.Account;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryLedgerRepository implements LedgerRepository {

    private final Map<Long, Account> accounts = new HashMap<>();

    @Override
    public Optional<Account> findAccount(Long userId) {
        return Optional.ofNullable(accounts.get(userId)).map(Account::copy);
    }

    @Override
    public void saveAccount(Long userId, Account account) {
        accounts.put(userId, account.copy());
    }
}
