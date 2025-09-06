package com.financial.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for bulk settlement status updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSettlementStatusResponse {
    
    private Integer totalRequested;
    private Integer totalUpdated;
    private List<BulkSettlementResult> results;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkSettlementResult {
        private String settlementId;
        private Boolean success;
        private String errorMessage;
        private String updatedStatus;
    }
}
