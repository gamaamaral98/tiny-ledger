package com.example.teyaledger.model.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
    UUID id,
    Instant timestamp,
    TransactionType type,
    BigDecimal amount
) {}
