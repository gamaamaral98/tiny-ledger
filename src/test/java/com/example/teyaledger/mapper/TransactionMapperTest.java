package com.example.teyaledger.mapper;

import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;
import com.example.teyaledger.model.dto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = new TransactionMapper();

    @Test
    void shouldConvertDomainTransactionToDTOTransactionResponse() {
        Transaction transaction = new Transaction(UUID.randomUUID(), Instant.now(), TransactionType.DEPOSIT, new BigDecimal("100.00"));
        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertNotNull(response);
        assertEquals(transaction.id(), response.id());
        assertEquals(transaction.timestamp(), response.timestamp());
        assertEquals(transaction.type(), response.type());
        assertEquals(transaction.amount(), response.amount());
    }

    @Test
    void shouldConvertMultipleDomainTransactionToDTOTransactionResponse() {
        Transaction transaction1 = new Transaction(UUID.randomUUID(), Instant.now(), TransactionType.DEPOSIT, new BigDecimal("100.00"));
        Transaction transaction2 = new Transaction(UUID.randomUUID(), Instant.now(), TransactionType.WITHDRAWAL, new BigDecimal("50.00"));

        var transactions = List.of(transaction1, transaction2);
        var responses = transactionMapper.toResponses(transactions);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(transaction1.id(), responses.get(0).id());
        assertEquals(transaction2.id(), responses.get(1).id());
    }

    @Test
    void shouldReturnEmptyList() {
        var responses = transactionMapper.toResponses(List.of());
        assertNotNull(responses);
        assertEquals(0, responses.size());
    }
}
