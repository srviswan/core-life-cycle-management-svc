package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response model for cash flow calculations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowResponse {
    
    private String requestId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate calculationDate;
    
    private DateRange dateRange;
    
    private CalculationType calculationType;
    
    private CalculationSummary summary;
    
    private List<ContractResult> contractResults;
    
    private List<CashFlow> cashFlows;
    
    private CalculationMetadata metadata;
    
    private String status;
    
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    public static CashFlowResponse error(String errorMessage) {
        return CashFlowResponse.builder()
                .status("ERROR")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fromDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate toDate;
    }
    
    public enum CalculationType {
        REAL_TIME_PROCESSING,
        HISTORICAL_RECALCULATION,
        BATCH_PROCESSING
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationSummary {
        private Integer totalContracts;
        private Integer totalCashFlows;
        private BigDecimal totalAmount;
        private String currency;
        private Long processingTimeMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractResult {
        private String contractId;
        private String underlying;
        private String index;
        private String type;
        private String currency;
        private BigDecimal totalPnl;
        private BigDecimal totalInterest;
        private BigDecimal totalDividends;
        private BigDecimal totalCashFlows;
        private List<PositionResult> positions; // Hierarchical: Contract -> Positions
        private List<CashFlow> cashFlows; // Legacy support
        private String status;
        private String errorMessage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionResult {
        private String positionId;
        private String product;
        private String underlying;
        private String type;
        private BigDecimal notionalAmount;
        private String currency;
        private BigDecimal totalPnl;
        private BigDecimal totalInterest;
        private BigDecimal totalDividends;
        private BigDecimal totalCashFlows;
        private List<CashFlow> lots; // Hierarchical: Position -> Lots
        private String status;
        private String errorMessage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlow {
        private String cashFlowId;
        private String requestId;
        private String contractId;
        private String positionId;
        private String lotId;
        private String scheduleId;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate calculationDate;
        
        private String cashFlowType;
        private BigDecimal equityLegAmount;
        private BigDecimal interestLegAmount;
        private BigDecimal totalAmount;
        private String currency;
        private String state;
        
        // Equity details
        private BigDecimal equityUnrealizedPnl;
        private BigDecimal equityRealizedPnl;
        private BigDecimal equityTotalPnl;
        private BigDecimal equityDividendAmount;
        private BigDecimal equityWithholdingTax;
        private BigDecimal equityNetDividend;
        
        // Interest details
        private BigDecimal interestAccruedAmount;
        private BigDecimal interestRate;
        private BigDecimal interestNotionalAmount;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdTimestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationMetadata {
        private String calculationVersion;
        private String calculationEngine;
        private Long processingTimeMs;
        private Long memoryUsageMB;
        private String dataSource;
        private Integer contractsProcessed;
        private Integer errorsEncountered;
    }
}
