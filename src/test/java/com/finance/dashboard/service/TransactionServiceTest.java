package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.TransactionFilterRequest;
import com.finance.dashboard.dto.request.TransactionRequest;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.User;
import com.finance.dashboard.model.enums.Role;
import com.finance.dashboard.model.enums.TransactionType;
import com.finance.dashboard.model.enums.UserStatus;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository        userRepository;

    @InjectMocks TransactionService transactionService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@finance.com")
                .password("encoded")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ── getById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns transaction when it exists and is not deleted")
    void getById_success() {
        Transaction txn = buildTransaction(1L, BigDecimal.valueOf(1000), TransactionType.INCOME);
        when(transactionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(txn));

        TransactionResponse response = transactionService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for soft-deleted or missing transaction")
    void getById_notFound() {
        when(transactionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("create saves and returns a new transaction")
    void create_success() {
        mockSecurityContext("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        TransactionRequest req = buildRequest(BigDecimal.valueOf(5000), TransactionType.EXPENSE, "Rent");
        Transaction saved = buildTransaction(10L, req.getAmount(), req.getType());
        saved.setCategory(req.getCategory());

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.create(req);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCategory()).isEqualTo("Rent");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ── delete (soft) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("delete marks transaction as deleted without removing from DB")
    void delete_softDeletes() {
        Transaction txn = buildTransaction(5L, BigDecimal.valueOf(200), TransactionType.EXPENSE);
        when(transactionRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(txn));
        when(transactionRepository.save(any())).thenReturn(txn);

        transactionService.delete(5L);

        assertThat(txn.isDeleted()).isTrue();
        verify(transactionRepository).save(txn);
    }

    @Test
    @DisplayName("delete throws when transaction not found")
    void delete_notFound() {
        when(transactionRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getTransactions ───────────────────────────────────────────────────

    @Test
    @DisplayName("getTransactions respects page size cap of 100")
    void getTransactions_pageSizeCapped() {
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setSize(999); // should be clamped to 100
        filter.setPage(0);

        when(transactionRepository.findAllWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        transactionService.getTransactions(filter);

        verify(transactionRepository).findAllWithFilters(
                isNull(), isNull(), isNull(), isNull(),
                argThat(p -> p.getPageSize() == 100)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Transaction buildTransaction(Long id, BigDecimal amount, TransactionType type) {
        return Transaction.builder()
                .id(id)
                .amount(amount)
                .type(type)
                .category("General")
                .date(LocalDate.now().minusDays(1))
                .createdBy(adminUser)
                .deleted(false)
                .build();
    }

    private TransactionRequest buildRequest(BigDecimal amount, TransactionType type, String category) {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(amount);
        req.setType(type);
        req.setCategory(category);
        req.setDate(LocalDate.now().minusDays(1));
        return req;
    }

    private void mockSecurityContext(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
