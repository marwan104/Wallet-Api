package com.marwan.walletapi.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullWalletFlow_fromRegistrationToTransfer() throws Exception {
        String token = register("Test User", "tester@walletapi.io", "Password@1");

        // Wallet starts at zero
        JsonNode me = getJson(get("/api/v1/wallets/me").header("Authorization", bearer(token)), 200);
        assertThat(new BigDecimal(me.path("balance").asText())).isEqualByComparingTo("0");

        // Deposit 100 -> 100
        JsonNode afterDeposit = getJson(post("/api/v1/wallets/deposit")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100.00}"), 200);
        assertThat(new BigDecimal(afterDeposit.path("balance").asText())).isEqualByComparingTo("100");

        // Withdraw 30 -> 70
        JsonNode afterWithdraw = getJson(post("/api/v1/wallets/withdraw")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":30.00}"), 200);
        assertThat(new BigDecimal(afterWithdraw.path("balance").asText())).isEqualByComparingTo("70");

        // Transfer 20 to the seeded demo wallet -> 50
        JsonNode afterTransfer = getJson(post("/api/v1/wallets/transfer")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toEmail\":\"sara@walletapi.io\",\"amount\":20.00}"), 200);
        assertThat(new BigDecimal(afterTransfer.path("balance").asText())).isEqualByComparingTo("50");

        // History: 3 transactions, newest first, with a full audit trail
        JsonNode history = getJson(get("/api/v1/wallets/transactions")
                .header("Authorization", bearer(token)), 200);
        assertThat(history.path("page").path("totalElements").asInt()).isEqualTo(3);
        JsonNode newest = history.path("content").get(0);
        assertThat(newest.path("type").asText()).isEqualTo("TRANSFER_OUT");
        assertThat(new BigDecimal(newest.path("amount").asText())).isEqualByComparingTo("20");
        assertThat(new BigDecimal(newest.path("balanceAfter").asText())).isEqualByComparingTo("50");
        assertThat(newest.path("counterpartyEmail").asText()).isEqualTo("sara@walletapi.io");
    }

    @Test
    void transfer_moreThanBalance_returns409() throws Exception {
        String token = register("Broke User", "broke@walletapi.io", "Password@1");

        mockMvc.perform(post("/api/v1/wallets/transfer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toEmail\":\"sara@walletapi.io\",\"amount\":999.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register("Dup User", "dup@walletapi.io", "Password@1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Dup User", "email", "dup@walletapi.io",
                                "password", "Password@1"))))
                .andExpect(status().isConflict());
    }

    @Test
    void securityRules_anonymousGets401_nonAdminGets403() throws Exception {
        String token = register("Plain User", "plain@walletapi.io", "Password@1");

        mockMvc.perform(get("/api/v1/wallets/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/wallets/admin/all").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    private String register(String fullName, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName, "email", email, "password", password))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private JsonNode getJson(MockHttpServletRequestBuilder builder, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
