package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse.CategoryTotal;
import com.finance.dashboard.dto.response.DashboardSummaryResponse.MonthlyTrend;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.model.enums.TransactionType;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;

    private static final int RECENT_LIMIT    = 10;
    private static final int TREND_MONTHS    = 6;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        // ── All-time totals ────────────────────────────────────────────────
        BigDecimal totalIncome   = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);

        // ── Current-month snapshot ─────────────────────────────────────────
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd   = LocalDate.now();

        BigDecimal currentMonthIncome   = transactionRepository
                .sumByTypeAndDateRange(TransactionType.INCOME, monthStart, monthEnd);
        BigDecimal currentMonthExpenses = transactionRepository
                .sumByTypeAndDateRange(TransactionType.EXPENSE, monthStart, monthEnd);
        BigDecimal currentMonthNet      = currentMonthIncome.subtract(currentMonthExpenses);

        // ── Category-wise totals ───────────────────────────────────────────
        List<CategoryTotal> categoryTotals = transactionRepository.categoryWiseTotals()
                .stream()
                .map(row -> CategoryTotal.builder()
                        .category((String) row[0])
                        .type(((TransactionType) row[1]).name())
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        // ── Monthly trends (last N months) ────────────────────────────────
        LocalDate trendsStart = LocalDate.now().minusMonths(TREND_MONTHS).withDayOfMonth(1);
        List<Object[]> rawTrends = transactionRepository.monthlyTrends(trendsStart);

        // Group rows by (year, month) since we get one row per type
        // Key: "year-month"
        Map<String, MonthlyTrend.MonthlyTrendBuilder> trendMap = new LinkedHashMap<>();

        for (Object[] row : rawTrends) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            TransactionType type  = (TransactionType) row[2];
            BigDecimal total      = (BigDecimal) row[3];

            String key = year + "-" + month;
            trendMap.putIfAbsent(key, MonthlyTrend.builder()
                    .year(year)
                    .month(month)
                    .monthLabel(monthLabel(year, month))
                    .income(BigDecimal.ZERO)
                    .expenses(BigDecimal.ZERO));

            MonthlyTrend.MonthlyTrendBuilder builder = trendMap.get(key);
            if (type == TransactionType.INCOME) {
                builder.income(total);
            } else {
                builder.expenses(total);
            }
        }

        List<MonthlyTrend> monthlyTrends = trendMap.values().stream()
                .map(b -> {
                    MonthlyTrend t = b.build();
                    t.setNet(t.getIncome().subtract(t.getExpenses()));
                    return t;
                })
                .sorted(Comparator.comparingInt(MonthlyTrend::getYear)
                        .thenComparingInt(MonthlyTrend::getMonth))
                .collect(Collectors.toList());

        // ── Recent activity ────────────────────────────────────────────────
        List<TransactionResponse> recent = transactionRepository
                .findRecent(PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .currentMonthIncome(currentMonthIncome)
                .currentMonthExpenses(currentMonthExpenses)
                .currentMonthNet(currentMonthNet)
                .categoryTotals(categoryTotals)
                .monthlyTrends(monthlyTrends)
                .recentTransactions(recent)
                .build();
    }

    /** Formats a year/month pair as "Jan 2025". */
    private String monthLabel(int year, int month) {
        return java.time.Month.of(month)
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year;
    }
}
