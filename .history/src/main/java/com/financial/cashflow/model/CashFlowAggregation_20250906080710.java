package com.financial.cashflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregated cash flow data for consolidation at different levels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowAggregation {
    
    private String contractId;
    private String positionId;
    private String currency;
    
    // Aggregated amounts
    private BigDecimal totalAmount;
    private BigDecimal equityLegAmount;
    private BigDecimal interestLegAmount;
    
    // P&L breakdown
    private BigDecimal equityUnrealizedPnl;
    private BigDecimal equityRealizedPnl;
    private BigDecimal equityTotalPnl;
    
    // Dividend breakdown
    private BigDecimal equityDividendAmount;
    private BigDecimal equityWithholdingTax;
    private BigDecimal equityNetDividend;
    
    // Interest breakdown
    private BigDecimal interestAccruedAmount;
    private BigDecimal interestNotionalAmount;
    
    // Counts for traceability
    private Integer positionCount; // Number of positions (for contract-level aggregation)
    private Integer lotCount;      // Number of lots (for position-level aggregation)
    private Integer cashFlowCount; // Number of individual cash flow records
}
