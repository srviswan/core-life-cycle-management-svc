package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Model to track withholding tax information for tax utility reporting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithholdingTaxInfo {
    
    private String contractId;
    private String lotId; // Optional - for lot-based calculations
    private String underlying;
    private String currency;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate exDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;
    
    private Double grossDividendAmount;
    private Double withholdingTaxRate; // Percentage (e.g., 15.0 for 15%)
    private Double withholdingTaxAmount;
    private Double netDividendAmount;
    
    private WithholdingTreatment withholdingTreatment;
    private String taxJurisdiction; // Country/region where tax is withheld
    private String taxUtilityReference; // Reference for tax utility reporting
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate calculationDate;
    
    private String calculationType; // "LOT_BASED" or "CONTRACT_BASED"
    
    public enum WithholdingTreatment {
        GROSS_UP,           // Dividend amount is gross, withholding tax is deducted
        NET_AMOUNT,         // Dividend amount is net after withholding tax
        NO_WITHHOLDING,    // No withholding tax applies
        TAX_CREDIT         // Withholding tax can be claimed as tax credit
    }
}
