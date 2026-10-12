package com.example.teyaledger.controller;

import com.example.teyaledger.mapper.TransactionMapper;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.dto.TransactionRequest;
import com.example.teyaledger.model.dto.TransactionResponse;
import com.example.teyaledger.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ledger/{userId}")
public class LedgerController {

    private final LedgerService ledgerService;
    private final TransactionMapper transactionMapper;

    public LedgerController(LedgerService ledgerService, TransactionMapper transactionMapper) {
        this.ledgerService = ledgerService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("/balance")
    public BigDecimal getBalance(@PathVariable @Positive Long userId) {
        return ledgerService.getBalance(userId);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable @Positive Long userId) {
        return transactionMapper.toResponses(ledgerService.getTransactions(userId));
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> makeTransaction(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction = ledgerService.makeTransaction(userId, request.type(), request.amount());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(transaction));
    }
}
