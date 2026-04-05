package com.finance.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.dto.request.LoginRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Dashboard Controller Integration Tests")
class DashboardControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Any authenticated user can view summary")
    void summaryAccessibleToAllRoles() throws Exception {
        for (String[] creds : new String[][]{
                {"admin", "admin123"},
                {"analyst", "analyst123"},
                {"viewer", "viewer123"}
        }) {
            String token = login(creds[0], creds[1]);
            mockMvc.perform(get("/api/dashboard/summary")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalIncome").isNumber())
                    .andExpect(jsonPath("$.data.totalExpenses").isNumber())
                    .andExpect(jsonPath("$.data.netBalance").isNumber())
                    .andExpect(jsonPath("$.data.categoryTotals").isArray())
                    .andExpect(jsonPath("$.data.monthlyTrends").isArray())
                    .andExpect(jsonPath("$.data.recentTransactions").isArray());
        }
    }

    @Test
    @DisplayName("Unauthenticated access to summary returns 401")
    void summaryRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }
}
