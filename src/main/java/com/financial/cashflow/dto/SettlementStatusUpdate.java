package com.financial.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for updating settlement status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementStatusUpdate {
    
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    private LocalDate actualSettlementDate;
    private String settlementReference;
    private String notes;
    private String retryReason;
    private LocalDateTime nextRetryDate;
    private String cancellationReason;
    private String cancelledBy;
}
