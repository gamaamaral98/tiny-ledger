package com.example.teyaledger.controller;

import com.example.teyaledger.mapper.TransactionMapper;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.dto.TransactionRequest;
import com.example.teyaledger.model.dto.TransactionResponse;
import com.example.teyaledger.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    public BigDecimal getBalance(@PathVariable Long userId) {
        return ledgerService.getBalance(userId);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable Long userId) {
        return transactionMapper.toResponses(ledgerService.getTransactions(userId));
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> makeTransaction(
            @PathVariable Long userId,
            @RequestHeader UUID uuid,
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction = ledgerService.makeTransaction(uuid, userId, request.type(), request.amount());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(transaction));
    }
}
