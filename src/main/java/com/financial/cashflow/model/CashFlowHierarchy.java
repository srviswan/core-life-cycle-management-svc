package com.financial.cashflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Hierarchical cash flow breakdown showing contract -> position -> lot structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowHierarchy {
    
    private String contractId;
    private String positionId;
    private String lotId;
    private String currency;
    
    // Aggregated amounts for this level
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
    
    // Count for traceability
    private Integer cashFlowCount; // Number of individual cash flow records at this level
}
