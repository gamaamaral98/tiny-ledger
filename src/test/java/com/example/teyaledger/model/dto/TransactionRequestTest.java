package com.example.teyaledger.model.dto;

import com.example.teyaledger.model.domain.TransactionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1", "100.50", "999999.99"})
    void shouldBeValidRequest(String amount) {
        var request = new TransactionRequest(TransactionType.DEPOSIT, new BigDecimal(amount));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullType() {
        var violations = validator.validate(new TransactionRequest(null, new BigDecimal("10.00")));

        assertEquals(1, violations.size());
        assertEquals("type", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void shouldRejectNullAmount() {
        var violations = validator.validate(new TransactionRequest(TransactionType.DEPOSIT, null));

        assertEquals(1, violations.size());
        assertEquals("amount", violations.iterator().next().getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void shouldRejectNullZeroOrNegativeAmount(String amount) {
        var violations = validator.validate(new TransactionRequest(TransactionType.DEPOSIT, new BigDecimal(amount)));

        assertEquals(1, violations.size());
        assertEquals("Amount must be greater than zero!", violations.iterator().next().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.001", "1000000.00"})
    void shouldRejectAmountExceedingLimitDigits(String amount) {
        var violations = validator.validate(new TransactionRequest(TransactionType.WITHDRAWAL, new BigDecimal(amount)));

        assertEquals(1, violations.size());
        assertEquals("amount", violations.iterator().next().getPropertyPath().toString());
    }
}
