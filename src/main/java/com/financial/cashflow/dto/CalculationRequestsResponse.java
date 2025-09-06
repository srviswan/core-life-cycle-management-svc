package com.financial.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for calculation requests queries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationRequestsResponse {
    
    private List<CalculationRequestInfo> calculationRequests;
    private SettlementResponse.PaginationInfo pagination;
    private Summary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationRequestInfo {
        private String requestId;
        private String contractId;
        private String calculationType;
        private String fromDate;
        private String toDate;
        private String status;
        private Integer progressPercentage;
        private String errorMessage;
        private String inputDataHash;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer totalRequests;
        private StatusBreakdown statusBreakdown;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class StatusBreakdown {
            private Integer processing;
            private Integer completed;
            private Integer failed;
            private Integer cancelled;
        }
    }
}
