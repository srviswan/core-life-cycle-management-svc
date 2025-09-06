package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Settlement instruction model following API specification
 */
@Entity
@Table(name = "settlement_instructions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInstruction {
    
    // Synthetic key
    private String settlementId;
    
    // Natural key components
    private String contractId;
    private String cashFlowId;
    private LocalDate settlementDate;
    private String settlementType; // INTEREST, EQUITY, DIVIDEND, PRINCIPAL
    
    // Settlement details
    private String counterparty;
    private BigDecimal amount;
    private String currency;
    
    // Status management
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    private Integer retryCount;
    private LocalDateTime lastRetryDate;
    private LocalDateTime nextRetryDate;
    private String errorMessage;
    
    // Completion details
    private LocalDate actualSettlementDate;
    private String settlementReference;
    private String notes;
    
    // Cancellation details
    private String cancelledBy;
    private String cancellationReason;
    
    // Audit fields
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    public enum Status {
        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
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
        INTEREST("INTEREST"),
        EQUITY("EQUITY"),
        DIVIDEND("DIVIDEND"),
        PRINCIPAL("PRINCIPAL");
        
        private final String value;
        
        SettlementType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
