package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/summary
     *
     * Returns a comprehensive dashboard payload including:
     *   - All-time income, expenses, net balance
     *   - Current-month income, expenses, net
     *   - Category-wise totals (income + expense)
     *   - Monthly trends for the last 6 months
     *   - 10 most recent transactions
     *
     * Requires: VIEWER, ANALYST, ADMIN
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
