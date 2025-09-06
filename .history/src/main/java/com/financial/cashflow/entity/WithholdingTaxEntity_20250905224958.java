package com.financial.cashflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing withholding tax information
 */
@Entity
@Table(name = "withholding_tax_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithholdingTaxEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "contract_id", nullable = false)
    private String contractId;
    
    @Column(name = "lot_id")
    private String lotId;
    
    @Column(name = "underlying", nullable = false)
    private String underlying;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "ex_date")
    private LocalDate exDate;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Column(name = "gross_dividend_amount", nullable = false)
    private Double grossDividendAmount;
    
    @Column(name = "withholding_tax_rate")
    private Double withholdingTaxRate;
    
    @Column(name = "withholding_tax_amount")
    private Double withholdingTaxAmount;
    
    @Column(name = "net_dividend_amount", nullable = false)
    private Double netDividendAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "withholding_treatment")
    private WithholdingTreatment withholdingTreatment;
    
    @Column(name = "tax_jurisdiction")
    private String taxJurisdiction;
    
    @Column(name = "tax_utility_reference")
    private String taxUtilityReference;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @Column(name = "calculation_type", nullable = false)
    private String calculationType;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum WithholdingTreatment {
        GROSS_UP,           // Dividend amount is gross, withholding tax is deducted
        NET_AMOUNT,         // Dividend amount is net after withholding tax
        NO_WITHHOLDING,    // No withholding tax applies
        TAX_CREDIT         // Withholding tax can be claimed as tax credit
    }
}
