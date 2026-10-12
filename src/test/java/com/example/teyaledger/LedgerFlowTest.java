package com.example.teyaledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerFlowTest {

    private static final AtomicLong NEXT_USER_ID = new AtomicLong(1000);

    @Autowired
    private MockMvc mockMvc;

    private final long userId = NEXT_USER_ID.getAndIncrement();

    @Test
    void depositsAndWithdrawalsAreReflectedInBalanceAndHistory() throws Exception {
        deposit("100.00").andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00));
        withdraw("30.50").andExpect(status().isCreated());
        deposit("5.25").andExpect(status().isCreated());

        mockMvc.perform(get(url("/balance")))
                .andExpect(status().isOk())
                .andExpect(content().string("74.75"));

        mockMvc.perform(get(url("/transactions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].amount").value(5.25))
                .andExpect(jsonPath("$[1].amount").value(30.50))
                .andExpect(jsonPath("$[2].amount").value(100.00));
    }

    @Test
    void accountsAreIsolatedFromEachOther() throws Exception {
        deposit("50.00").andExpect(status().isCreated());

        mockMvc.perform(post("/ledger/{userId}/transactions", userId + 100_000)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DEPOSIT", "7.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get(url("/balance"))).andExpect(content().string("50.00"));
    }

    @Test
    void withdrawalExceedingBalanceIsRejectedAndLeavesAccountUntouched() throws Exception {
        deposit("20.00").andExpect(status().isCreated());

        withdraw("20.01").andExpect(status().isUnprocessableContent());

        mockMvc.perform(get(url("/balance"))).andExpect(content().string("20.00"));
        mockMvc.perform(get(url("/transactions")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void withdrawingTheFullBalanceIsAllowed() throws Exception {
        deposit("20.00").andExpect(status().isCreated());
        withdraw("20.00").andExpect(status().isCreated());

        mockMvc.perform(get(url("/balance"))).andExpect(content().string("0.00"));
    }

    @Test
    void unknownAccountReturnsNotFound() throws Exception {
        mockMvc.perform(get(url("/balance"))).andExpect(status().isNotFound());
        mockMvc.perform(get(url("/transactions"))).andExpect(status().isNotFound());
        withdraw("10.00").andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsReturnBadRequestAndDoNotCreateAccount() throws Exception {
        deposit("0").andExpect(status().isBadRequest());
        deposit("-5.00").andExpect(status().isBadRequest());
        deposit("1.234").andExpect(status().isBadRequest());
        deposit("1234567.00").andExpect(status().isBadRequest());
        mockMvc.perform(post(url("/transactions"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10.00}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(url("/transactions"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TRANSFER", "10.00")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(url("/balance"))).andExpect(status().isNotFound());
    }

    private ResultActions deposit(String amount) throws Exception {
        return send("DEPOSIT", amount);
    }

    private ResultActions withdraw(String amount) throws Exception {
        return send("WITHDRAWAL", amount);
    }

    private ResultActions send(String type, String amount) throws Exception {
        return mockMvc.perform(post(url("/transactions"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(type, amount)));
    }

    private String url(String path) {
        return "/ledger/" + userId + path;
    }

    private String body(String type, String amount) {
        return "{\"type\": \"%s\", \"amount\": %s}".formatted(type, amount);
    }
}
