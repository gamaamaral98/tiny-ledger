package com.example.teyaledger.model.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record Transaction(
    UUID id,
    LocalDateTime timestamp,
    TransactionType type,
    BigDecimal amount
) {}
