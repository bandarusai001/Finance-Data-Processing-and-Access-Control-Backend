package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.TransactionFilterRequest;
import com.finance.dashboard.dto.request.TransactionRequest;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository        userRepository;

    /* ── List with filters ─────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter) {
        // Clamp page size to prevent runaway queries
        int size = Math.min(filter.getSize(), 100);
        PageRequest pageable = PageRequest.of(filter.getPage(), size);

        return transactionRepository.findAllWithFilters(
                filter.getType(),
                filter.getCategory(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable
        ).map(TransactionResponse::from);
    }

    /* ── Read single ───────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return TransactionResponse.from(findActiveOrThrow(id));
    }

    /* ── Create ────────────────────────────────────────────────────────── */

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User currentUser = resolveCurrentUser();

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(currentUser)
                .build();

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    /* ── Update ────────────────────────────────────────────────────────── */

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = findActiveOrThrow(id);

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    /* ── Soft Delete ───────────────────────────────────────────────────── */

    @Transactional
    public void delete(Long id) {
        Transaction transaction = findActiveOrThrow(id);
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
    }

    /* ── Helpers ───────────────────────────────────────────────────────── */

    private Transaction findActiveOrThrow(Long id) {
        return transactionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    /**
     * Resolves the currently authenticated user from the security context.
     * This is safe because all write endpoints are behind JWT authentication.
     */
    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }
}
