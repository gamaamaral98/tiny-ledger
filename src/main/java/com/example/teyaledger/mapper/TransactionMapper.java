package com.example.teyaledger.mapper;

import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.timestamp(),
                transaction.type(),
                transaction.amount());
    }

    public List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream().map(this::toResponse).toList();
    }
}
