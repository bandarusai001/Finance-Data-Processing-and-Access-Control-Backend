package com.finance.dashboard.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
public class DashboardSummaryResponse {

    // ── Top-level totals ──────────────────────────────────────
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    // ── Category breakdown ────────────────────────────────────
    private List<CategoryTotal> categoryTotals;

    // ── Monthly trends (last 6 months) ────────────────────────
    private List<MonthlyTrend> monthlyTrends;

    // ── Recent activity ───────────────────────────────────────
    private List<TransactionResponse> recentTransactions;

    // ── Current-month snapshot ────────────────────────────────
    private BigDecimal currentMonthIncome;
    private BigDecimal currentMonthExpenses;
    private BigDecimal currentMonthNet;

    // ──────────────────────────────────────────────────────────
    @Data
    @Builder
    public static class CategoryTotal {
        private String     category;
        private String     type;
        private BigDecimal total;
    }

    @Data
    @Builder
    public static class MonthlyTrend {
        private int        year;
        private int        month;
        private String     monthLabel;   // e.g. "Mar 2025"
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
