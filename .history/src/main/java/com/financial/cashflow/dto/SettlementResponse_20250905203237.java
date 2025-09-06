package com.financial.cashflow.dto;

import com.financial.cashflow.model.SettlementInstruction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for settlement queries with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    
    private List<SettlementInstruction> settlements;
    private PaginationInfo pagination;
    private SettlementSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementSummary {
        private Integer totalSettlements;
        private Double totalAmount;
        private String currency;
        private StatusBreakdown statusBreakdown;
        private TypeBreakdown typeBreakdown;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class StatusBreakdown {
            private Integer pending;
            private Integer processing;
            private Integer completed;
            private Integer failed;
            private Integer cancelled;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TypeBreakdown {
            private Integer interest;
            private Integer equity;
            private Integer dividend;
            private Integer principal;
        }
    }
}
