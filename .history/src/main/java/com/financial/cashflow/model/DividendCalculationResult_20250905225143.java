package com.financial.cashflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of dividend calculation including withholding tax details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DividendCalculationResult {
    
    private double totalDividendAmount;
    private List<WithholdingTaxInfo> withholdingTaxDetails;
    
    /**
     * Get total withholding tax amount across all dividends
     */
    public double getTotalWithholdingTaxAmount() {
        if (withholdingTaxDetails == null) {
            return 0.0;
        }
        return withholdingTaxDetails.stream()
                .mapToDouble(WithholdingTaxInfo::getWithholdingTaxAmount)
                .sum();
    }
    
    /**
     * Get total gross dividend amount before withholding tax
     */
    public double getTotalGrossDividendAmount() {
        if (withholdingTaxDetails == null) {
            return totalDividendAmount;
        }
        return withholdingTaxDetails.stream()
                .mapToDouble(WithholdingTaxInfo::getGrossDividendAmount)
                .sum();
    }
}
