package com.finance.dashboard.dto.request;

import com.finance.dashboard.model.enums.TransactionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters for filtering the transaction list endpoint.
 * All fields are optional.
 */
@Data
public class TransactionFilterRequest {

    private TransactionType type;

    private String category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private int page = 0;

    private int size = 20;
}
