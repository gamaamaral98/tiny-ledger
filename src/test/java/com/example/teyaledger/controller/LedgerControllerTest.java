package com.example.teyaledger.controller;

import com.example.teyaledger.exceptions.AccountNotFoundException;
import com.example.teyaledger.exceptions.InsufficientFundsException;
import com.example.teyaledger.mapper.TransactionMapper;
import com.example.teyaledger.model.domain.Transaction;
import com.example.teyaledger.model.domain.TransactionType;
import com.example.teyaledger.model.dto.TransactionRequest;
import com.example.teyaledger.model.dto.TransactionResponse;
import com.example.teyaledger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {
    private static final Instant NOW = Instant.parse("2026-10-12T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private LedgerService ledgerService;
    @MockitoBean
    private TransactionMapper transactionMapper;

    @Test
    void shouldReturnBalance200() throws Exception {
        when(ledgerService.getBalance(1L)).thenReturn(new BigDecimal("100.00"));

        mockMvc.perform(get("/ledger/{userId}/balance", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("100.00"));
    }

    @Test
    void shouldNotReturnBalanceWhenUserIdNotNumeric400() throws Exception {
        mockMvc.perform(get("/ledger/{userId}/balance", "InvalidUserId"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnBalance400WhenUserIdNotPositive() throws Exception {
        mockMvc.perform(get("/ledger/{userId}/balance", -1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnBalanceAccountNotFound404() throws Exception {
        when(ledgerService.getBalance(1L)).thenThrow(AccountNotFoundException.class);

        mockMvc.perform(get("/ledger/{userId}/balance", 1))
                .andExpect(status().isNotFound());

        verifyNoInteractions(transactionMapper);
    }

    @Test
    void shouldReturnEmptyListOfTransactions200() throws Exception {
        when(ledgerService.getTransactions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/ledger/{userId}/transactions", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnTransactions200() throws Exception {
        Transaction transaction1 = new Transaction(UUID.randomUUID(), NOW.minus(365, ChronoUnit.DAYS), TransactionType.DEPOSIT, new BigDecimal("100.0"));
        Transaction transaction2 = new Transaction(UUID.randomUUID(), NOW.minus(180, ChronoUnit.DAYS), TransactionType.WITHDRAWAL, new BigDecimal("50.0"));
        List<Transaction> transactions = List.of(transaction1, transaction2);

        TransactionResponse transactionResponse1 = new TransactionResponse(transaction1.id(), transaction1.timestamp(), transaction1.type(), transaction1.amount());
        TransactionResponse transactionResponse2 = new TransactionResponse(transaction2.id(), transaction2.timestamp(), transaction2.type(), transaction2.amount());
        List<TransactionResponse> transactionResponses = List.of(transactionResponse1, transactionResponse2);

        when(ledgerService.getTransactions(1L)).thenReturn(transactions);
        when(transactionMapper.toResponses(transactions)).thenReturn(transactionResponses);

        mockMvc.perform(get("/ledger/{userId}/transactions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(transaction1.id().toString()))
                .andExpect(jsonPath("$[0].timestamp").value(transaction1.timestamp().toString()))
                .andExpect(jsonPath("$[0].type").value(transaction1.type().toString()))
                .andExpect(jsonPath("$[0].amount").value(transaction1.amount().toString()))
                .andExpect(jsonPath("$[1].id").value(transaction2.id().toString()))
                .andExpect(jsonPath("$[1].timestamp").value(transaction2.timestamp().toString()))
                .andExpect(jsonPath("$[1].type").value(transaction2.type().toString()))
                .andExpect(jsonPath("$[1].amount").value(transaction2.amount().toString()));
    }

    @Test
    void shouldNotReturnTransactionsWhenUserIdNotNumeric400() throws Exception {
        mockMvc.perform(get("/ledger/{userId}/transactions", "invalidUserId"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnTransactionsWhenUserIdNotPositive400() throws Exception {
        mockMvc.perform(get("/ledger/{userId}/transactions", -1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnTransactionsAccountNotFound404() throws Exception {
        when(ledgerService.getTransactions(1L)).thenThrow(AccountNotFoundException.class);

        mockMvc.perform(get("/ledger/{userId}/transactions", 1L))
                .andExpect(status().isNotFound());

        verifyNoInteractions(transactionMapper);
    }

    @Test
    void shouldReturnDeposit201() throws Exception {
        Transaction transaction = new Transaction(UUID.randomUUID(), NOW, TransactionType.DEPOSIT, new BigDecimal("100.00"));
        TransactionResponse response = new TransactionResponse(transaction.id(), transaction.timestamp(), transaction.type(), transaction.amount());

        when(ledgerService.makeTransaction(1L, TransactionType.DEPOSIT, new BigDecimal("100.00")))
                .thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.DEPOSIT, "100.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-12T10:15:30Z"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void shouldReturnWithdrawal201() throws Exception {
        Transaction transaction = new Transaction(UUID.randomUUID(), NOW, TransactionType.WITHDRAWAL, new BigDecimal("50.00"));
        TransactionResponse response = new TransactionResponse(transaction.id(), transaction.timestamp(), transaction.type(), transaction.amount());

        when(ledgerService.makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("50.00")))
                .thenReturn(transaction);
        when(transactionMapper.toResponse(transaction))
                .thenReturn(response);

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.WITHDRAWAL, "50.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-12T10:15:30Z"))
                .andExpect(jsonPath("$.amount").value(50.00));
    }

    @Test
    void shouldNotReturnTransactionWhenUserIdNotNumeric400() throws Exception {
        mockMvc.perform(post("/ledger/{userId}/transactions", "invalidUserId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.DEPOSIT, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnTransactionWhenUserIdNotPositive400() throws Exception {
        mockMvc.perform(post("/ledger/{userId}/transactions", -1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.DEPOSIT, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", // Empty body
            "{\"type\":null,\"amount\":100.00}", // Null type
            "{\"amount\":100.00}", // Missing type
            "{\"type\":\"DEPOSIT\",\"amount\":null}", // Null amount
            "{\"type\":\"DEPOSIT\"}", // Missing amount
            "{\"type\":\"DEPOSIT\",\"amount\":0}", // Zero amount
            "{\"type\":\"DEPOSIT\",\"amount\":-5.00}", // Negative amount
            "{\"type\":\"DEPOSIT\",\"amount\":10.123}", // More than 2 decimal places
            "{\"type\":\"DEPOSIT\",\"amount\":1000000.00}", // Amount exceeding maximum
            "{\"type\":\"DEPOSIT\",\"amount\":\"abc\"}", // Non-numeric amount
            "{\"type\":\"DEPOSIT\",\"amount\":" // Malformed JSON
    })
    void shouldNotReturnTransactionForInvalidBody400(String body) throws Exception {
        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnWithdrawalTransactionWhenAccountNotFound404() throws Exception {
        when(ledgerService.makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("100.00")))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.WITHDRAWAL, "100.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"));

        verifyNoInteractions(transactionMapper);
    }

    @Test
    void shouldNotReturnTransactionWhenInsufficientFunds422() throws Exception {
        when(ledgerService.makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("1000.00")))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.WITHDRAWAL, "1000.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Insufficient funds"));

        verifyNoInteractions(transactionMapper);
    }

    private String requestBody(TransactionType type, String amount) {
        return objectMapper.writeValueAsString(new TransactionRequest(type, new BigDecimal(amount)));
    }
}
