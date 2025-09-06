package com.financial.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for bulk settlement status updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSettlementStatusUpdate {
    
    private List<String> settlementIds;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    private LocalDate actualSettlementDate;
    private String settlementReference;
    private String notes;
}
