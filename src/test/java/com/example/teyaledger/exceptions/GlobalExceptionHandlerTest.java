package com.example.teyaledger.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleInsufficientFundsException422WithMessage() {
        ProblemDetail result = handler.handleInsufficientFundsException(
                new InsufficientFundsException("Insufficient funds"));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), result.getStatus());
        assertEquals("Insufficient funds", result.getDetail());
    }

    @Test
    void shouldHandleAccountNotFoundException404WithMessage() {
        ProblemDetail result = handler.handleAccountNotFoundException(
                new AccountNotFoundException("Account not found"));

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Account not found", result.getDetail());
    }
}
