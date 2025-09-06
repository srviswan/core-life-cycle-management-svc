package com.financial.cashflow.dto;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for calculation reproduction results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationReproduction {
    
    private String requestId;
    private CashFlowRequest originalRequest;
    private CashFlowResponse reproducedResults;
    private IntegrityCheck integrityCheck;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrityCheck {
        private String inputDataHash;
        private Boolean hashMatch;
        private Boolean resultMatch;
        private List<String> differences;
    }
}
