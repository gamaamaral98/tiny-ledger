package com.example.teyaledger.model.dto;

import com.example.teyaledger.model.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    Instant timestamp,
    TransactionType type,
    BigDecimal amount
) {}
