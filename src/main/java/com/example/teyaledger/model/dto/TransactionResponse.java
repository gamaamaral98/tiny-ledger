package com.example.teyaledger.model.dto;

import com.example.teyaledger.model.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    LocalDateTime timestamp,
    TransactionType type,
    BigDecimal amount
) {}
