package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Dividend model for dividend calculations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dividend {
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate exDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;
    
    private Double amount;
    private String currency;
    private Double withholdingTaxRate; // Tax rate as percentage (e.g., 15.0 for 15%)
    private WithholdingTreatment withholdingTreatment; // How withholding is handled
    
    public enum WithholdingTreatment {
        GROSS_UP,           // Dividend amount is gross, withholding tax is deducted
        NET_AMOUNT,         // Dividend amount is net after withholding tax
        NO_WITHHOLDING,    // No withholding tax applies
        TAX_CREDIT         // Withholding tax can be claimed as tax credit
    }
}
