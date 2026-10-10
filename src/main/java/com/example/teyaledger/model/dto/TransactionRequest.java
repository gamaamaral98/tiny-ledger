package com.example.teyaledger.model.dto;

import com.example.teyaledger.model.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull
        TransactionType type,
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "Amount must be greater than zero!")
        @Digits(integer = 6, fraction = 2)
        BigDecimal amount
) {}
