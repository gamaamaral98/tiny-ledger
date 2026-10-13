package com.example.teyaledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Reset the application context because of in-memory approach
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LedgerFlowTest {

    private static final long USER_ID = 1000L;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldKeepAccountsSeparate() throws Exception {

        long OTHER_USER_ID = USER_ID + 100_000;

        deposit("50.00")
                .andExpect(status().isCreated());

        mockMvc.perform(post("/ledger/{userId}/transactions", OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DEPOSIT", "50.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get(url("/balance")))
                .andExpect(content().string("50.00"));
    }

    @Test
    void shouldReflectTransactionsOnBalanceAndTransactionHistory() throws Exception {
        deposit("100.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00));

        withdraw("30.50")
                .andExpect(status().isCreated());

        deposit("5.25")
                .andExpect(status().isCreated());

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
    void shouldWithdrawWholeBalance() throws Exception {
        deposit("20.00")
                .andExpect(status().isCreated());

        withdraw("20.00")
                .andExpect(status().isCreated());

        mockMvc.perform(get(url("/balance")))
                .andExpect(content().string("0.00"));
    }

    @Test
    void shouldNotWithdrawWithInsufficientFunds() throws Exception {
        deposit("20.00")
                .andExpect(status().isCreated());

        withdraw("20.01")
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get(url("/balance")))
                .andExpect(content().string("20.00"));

        mockMvc.perform(get(url("/transactions")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnNotFoundOnNonExistentAccount() throws Exception {
        mockMvc.perform(get(url("/balance")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(url("/transactions")))
                .andExpect(status().isNotFound());

        withdraw("10.00").andExpect(status()
                .isNotFound());
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
        return "/ledger/" + USER_ID + path;
    }

    private String body(String type, String amount) {
        return "{\"type\": \"%s\", \"amount\": %s}".formatted(type, amount);
    }
}
