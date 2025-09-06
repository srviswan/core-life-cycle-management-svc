package com.financial.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating settlement instructions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreateRequest {
    
    private String contractId;
    private String cashFlowId;
    private LocalDate settlementDate;
    private String settlementType; // INTEREST, EQUITY, DIVIDEND, PRINCIPAL
    private String counterparty;
    private Double amount;
    private String currency;
    private String notes;
}
