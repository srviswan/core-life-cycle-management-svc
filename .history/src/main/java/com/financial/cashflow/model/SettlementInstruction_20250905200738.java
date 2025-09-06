package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Settlement instruction model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInstruction {
    
    private String instructionId;
    
    private String contractId;
    
    private String counterparty;
    
    private String currency;
    
    private BigDecimal amount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate settlementDate;
    
    private String status;
    
    private String settlementType;
    
    private String accountNumber;
    
    private String bankCode;
    
    private String reference;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdTimestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedTimestamp;
    
    private String errorMessage;
    
    public enum Status {
        PENDING("PENDING"),
        PROCESSED("PROCESSED"),
        FAILED("FAILED"),
        CANCELLED("CANCELLED");
        
        private final String value;
        
        Status(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum SettlementType {
        CASH_SETTLEMENT("CASH_SETTLEMENT"),
        PHYSICAL_DELIVERY("PHYSICAL_DELIVERY"),
        NET_SETTLEMENT("NET_SETTLEMENT");
        
        private final String value;
        
        SettlementType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
