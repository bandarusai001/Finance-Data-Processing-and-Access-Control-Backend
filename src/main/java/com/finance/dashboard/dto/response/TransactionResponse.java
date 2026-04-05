package com.finance.dashboard.dto.response;

import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long            id;
    private BigDecimal      amount;
    private TransactionType type;
    private String          category;
    private LocalDate       date;
    private String          notes;
    private String          createdBy;
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .notes(t.getNotes())
                .createdBy(t.getCreatedBy().getUsername())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
