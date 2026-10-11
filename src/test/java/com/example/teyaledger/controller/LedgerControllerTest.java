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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {
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
    void shouldNotReturnBalance400() throws Exception {
        mockMvc.perform(get("/ledger/{userId}/balance", "invalidUserId"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotReturnBalance404() throws Exception {
        when(ledgerService.getBalance(1L)).thenThrow(AccountNotFoundException.class);

        mockMvc.perform(get("/ledger/{userId}/balance", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNoTransactions200() throws Exception {
        when(ledgerService.getTransactions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/ledger/{userId}/transactions", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnTransactions200() throws Exception {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Transaction transaction1 = new Transaction(UUID.randomUUID(), now.minusYears(1), TransactionType.DEPOSIT, new BigDecimal("100.0"));
        Transaction transaction2 = new Transaction(UUID.randomUUID(), now.minusMonths(6), TransactionType.WITHDRAWAL, new BigDecimal("50.0"));
        Transaction transaction3 = new Transaction(UUID.randomUUID(), now.minusDays(1), TransactionType.DEPOSIT, new BigDecimal("200.0"));
        List<Transaction> transactions = List.of(transaction1, transaction2, transaction3);

        when(ledgerService.getTransactions(1L)).thenReturn(transactions);
        when(transactionMapper.toResponses(transactions)).thenReturn(transactions.stream()
                .map(t -> new TransactionResponse(t.id(), t.timestamp(), t.type(), t.amount()))
                .toList());

        mockMvc.perform(get("/ledger/{userId}/transactions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(transaction1.id().toString()))
                .andExpect(jsonPath("$[0].timestamp").value(transaction1.timestamp().toString()))
                .andExpect(jsonPath("$[0].type").value(transaction1.type().toString()))
                .andExpect(jsonPath("$[0].amount").value(transaction1.amount().toString()))
                .andExpect(jsonPath("$[1].id").value(transaction2.id().toString()))
                .andExpect(jsonPath("$[1].timestamp").value(transaction2.timestamp().toString()))
                .andExpect(jsonPath("$[1].type").value(transaction2.type().toString()))
                .andExpect(jsonPath("$[1].amount").value(transaction2.amount().toString()))
                .andExpect(jsonPath("$[2].id").value(transaction3.id().toString()))
                .andExpect(jsonPath("$[2].timestamp").value(transaction3.timestamp().toString()))
                .andExpect(jsonPath("$[2].type").value(transaction3.type().toString()))
                .andExpect(jsonPath("$[2].amount").value(transaction3.amount().toString()));
    }

    @Test
    void shouldNotReturnTransactions400() throws Exception {
        mockMvc.perform(get("/ledger/{userId}/transactions", "invalidUserId"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotReturnTransactions404() throws Exception {
        when(ledgerService.getTransactions(1L)).thenThrow(AccountNotFoundException.class);

        mockMvc.perform(get("/ledger/{userId}/transactions", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnDeposit201() throws Exception {
        Transaction transaction = new Transaction(UUID.randomUUID(), LocalDateTime.now(), TransactionType.DEPOSIT, new BigDecimal("100.00"));
        TransactionResponse response = new TransactionResponse(transaction.id(), transaction.timestamp(), transaction.type(), transaction.amount());

        when(ledgerService.makeTransaction(1L, TransactionType.DEPOSIT, new BigDecimal("100.00")))
                .thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.DEPOSIT, "100.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        verify(ledgerService).makeTransaction(1L, TransactionType.DEPOSIT, new BigDecimal("100.00"));
    }

    @Test
    void shouldReturnWithdrawal201() throws Exception {
        Transaction transaction = new Transaction(UUID.randomUUID(), LocalDateTime.now(), TransactionType.WITHDRAWAL, new BigDecimal("50.00"));
        TransactionResponse response = new TransactionResponse(transaction.id(), transaction.timestamp(), transaction.type(), transaction.amount());

        when(ledgerService.makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("50.00")))
                .thenReturn(transaction);
        when(transactionMapper.toResponse(transaction))
                .thenReturn(response);

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.WITHDRAWAL, "50.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));

        verify(ledgerService).makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("50.00"));
    }

    @Test
    void shouldNotReturnTransaction400WhenUserIdInvalid() throws Exception {
        mockMvc.perform(post("/ledger/{userId}/transactions", "invalidUserId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.DEPOSIT, "100.00")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ledgerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"type\":\"DEPOSIT\"}",
            "{\"amount\":100.00}",
            "{\"type\":null,\"amount\":100.00}",
            "{\"type\":\"DEPOSIT\",\"amount\":null}",
            "{\"type\":\"DEPOSIT\",\"amount\":0}",
            "{\"type\":\"DEPOSIT\",\"amount\":-5.00}",
            "{\"type\":\"DEPOSIT\",\"amount\":10.123}",
            "{\"type\":\"DEPOSIT\",\"amount\":1000000.00}",
            "{\"type\":\"DEPOSIT\",\"amount\":\"abc\"}",
            "{\"type\":\"DEPOSIT\",\"amount\":",
            ""
    })
    void shouldNotReturnTransaction400ForInvalidBody(String body) throws Exception {
        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ledgerService);
    }

    @Test
    void shouldNotReturnTransaction404() throws Exception {
        when(ledgerService.makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("100.00")))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.WITHDRAWAL, "100.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void shouldNotReturnTransaction422() throws Exception {
        when(ledgerService.makeTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("1000.00")))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.WITHDRAWAL, "1000.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Insufficient funds"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "999999.99"})
    void shouldAcceptBoundaryAmounts(String amount) throws Exception {
        BigDecimal value = new BigDecimal(amount);
        Transaction transaction = new Transaction(UUID.randomUUID(), LocalDateTime.now(), TransactionType.DEPOSIT, value);

        when(ledgerService.makeTransaction(1L, TransactionType.DEPOSIT, value)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction))
                .thenReturn(new TransactionResponse(transaction.id(), transaction.timestamp(), transaction.type(), value));

        mockMvc.perform(post("/ledger/{userId}/transactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(TransactionType.DEPOSIT, amount)))
                .andExpect(status().isCreated());
    }

    private String requestBody(TransactionType type, String amount) {
        return objectMapper.writeValueAsString(new TransactionRequest(type, new BigDecimal(amount)));
    }
}
