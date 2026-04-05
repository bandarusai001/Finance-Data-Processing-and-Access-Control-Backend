package com.finance.dashboard.config;

import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.User;
import com.finance.dashboard.model.enums.Role;
import com.finance.dashboard.model.enums.TransactionType;
import com.finance.dashboard.model.enums.UserStatus;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the database with default users and sample transactions if empty.
 * This runs once on application startup.
 *
 * Default credentials:
 *   admin   / admin123   (ADMIN)
 *   analyst / analyst123 (ANALYST)
 *   viewer  / viewer123  (VIEWER)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initialization.");
            return;
        }

        log.info("Seeding default users and sample transactions...");

        // ── Users ──────────────────────────────────────────────────────────
        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@finance.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        User analyst = userRepository.save(User.builder()
                .username("analyst")
                .email("analyst@finance.com")
                .password(passwordEncoder.encode("analyst123"))
                .fullName("Financial Analyst")
                .role(Role.ANALYST)
                .status(UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .username("viewer")
                .email("viewer@finance.com")
                .password(passwordEncoder.encode("viewer123"))
                .fullName("Dashboard Viewer")
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build());

        // ── Sample Transactions ────────────────────────────────────────────
        LocalDate today = LocalDate.now();

        List<Transaction> samples = List.of(
            // Income
            txn(BigDecimal.valueOf(85000), TransactionType.INCOME, "Salary",       today.minusDays(2),  "Monthly salary",         admin),
            txn(BigDecimal.valueOf(12000), TransactionType.INCOME, "Freelance",    today.minusDays(10), "Freelance project payout", admin),
            txn(BigDecimal.valueOf(3500),  TransactionType.INCOME, "Investments",  today.minusDays(15), "Dividend income",        analyst),
            txn(BigDecimal.valueOf(75000), TransactionType.INCOME, "Salary",       today.minusMonths(1), "Previous month salary", admin),
            txn(BigDecimal.valueOf(8000),  TransactionType.INCOME, "Freelance",    today.minusMonths(1).minusDays(5), "Side project", analyst),

            // Expenses
            txn(BigDecimal.valueOf(25000), TransactionType.EXPENSE, "Rent",        today.minusDays(5),  "Monthly rent",           admin),
            txn(BigDecimal.valueOf(4200),  TransactionType.EXPENSE, "Groceries",   today.minusDays(3),  "Weekly grocery run",     admin),
            txn(BigDecimal.valueOf(1800),  TransactionType.EXPENSE, "Utilities",   today.minusDays(7),  "Electricity and water",  admin),
            txn(BigDecimal.valueOf(999),   TransactionType.EXPENSE, "Subscriptions", today.minusDays(1), "SaaS tools",            admin),
            txn(BigDecimal.valueOf(6500),  TransactionType.EXPENSE, "Travel",      today.minusDays(12), "Business trip",         analyst),
            txn(BigDecimal.valueOf(2100),  TransactionType.EXPENSE, "Dining",      today.minusDays(4),  "Team lunch",            admin),
            txn(BigDecimal.valueOf(22000), TransactionType.EXPENSE, "Rent",        today.minusMonths(1), "Prev month rent",      admin),
            txn(BigDecimal.valueOf(5500),  TransactionType.EXPENSE, "Groceries",   today.minusMonths(1).minusDays(3), "Monthly groceries", admin)
        );

        transactionRepository.saveAll(samples);
        log.info("Seeded {} users and {} transactions.", userRepository.count(), transactionRepository.count());
    }

    private Transaction txn(BigDecimal amount, TransactionType type, String category,
                             LocalDate date, String notes, User createdBy) {
        return Transaction.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .date(date)
                .notes(notes)
                .createdBy(createdBy)
                .build();
    }
}
