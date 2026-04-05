package com.finance.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.dto.request.LoginRequest;
import com.finance.dashboard.dto.request.TransactionRequest;
import com.finance.dashboard.model.enums.TransactionType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Transaction Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        adminToken  = login("admin",  "admin123");
        viewerToken = login("viewer", "viewer123");
    }

    // ── Read access ─────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Viewer can list transactions")
    void viewerCanRead() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Viewer can filter transactions by type")
    void filterByType() throws Exception {
        mockMvc.perform(get("/api/transactions?type=INCOME")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("INCOME"));
    }

    // ── Write access ─────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Admin can create a transaction")
    void adminCanCreate() throws Exception {
        TransactionRequest req = buildRequest();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.category").value("Test Category"));
    }

    @Test
    @Order(4)
    @DisplayName("Viewer cannot create a transaction (403)")
    void viewerCannotCreate() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("Unauthenticated request returns 401")
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("Create transaction with invalid amount returns 400")
    void invalidAmountReturns400() throws Exception {
        TransactionRequest req = buildRequest();
        req.setAmount(BigDecimal.ZERO);  // violates @DecimalMin

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.amount").exists());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    private TransactionRequest buildRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(BigDecimal.valueOf(5000));
        req.setType(TransactionType.EXPENSE);
        req.setCategory("Test Category");
        req.setDate(LocalDate.now().minusDays(1));
        req.setNotes("Integration test transaction");
        return req;
    }
}
