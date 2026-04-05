package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.TransactionFilterRequest;
import com.finance.dashboard.dto.request.TransactionRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * GET /api/transactions
     *   ?type=INCOME|EXPENSE
     *   &category=Rent
     *   &startDate=2025-01-01
     *   &endDate=2025-03-31
     *   &page=0&size=20
     *
     * Returns a paginated, filtered list of non-deleted transactions.
     * Requires: VIEWER, ANALYST, ADMIN
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactions(
            @ModelAttribute TransactionFilterRequest filter) {

        Page<TransactionResponse> page = transactionService.getTransactions(filter);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * GET /api/transactions/{id}
     * Retrieves a single transaction.
     * Requires: VIEWER, ANALYST, ADMIN
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.getById(id)));
    }

    /**
     * POST /api/transactions
     * Creates a new financial transaction.
     * Requires: ADMIN
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse created = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transaction created successfully", created));
    }

    /**
     * PUT /api/transactions/{id}
     * Fully replaces an existing transaction's fields.
     * Requires: ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse updated = transactionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Transaction updated successfully", updated));
    }

    /**
     * DELETE /api/transactions/{id}
     * Soft-deletes a transaction (sets deleted=true, record is retained in DB).
     * Requires: ADMIN
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Transaction deleted successfully", null));
    }
}
