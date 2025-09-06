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
    @Id
    @Column(name = "settlement_id")
    private String settlementId;
    
    // Natural key components
    @Column(name = "contract_id", nullable = false)
    private String contractId;
    
    @Column(name = "cash_flow_id", nullable = false)
    private String cashFlowId;
    
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;
    
    @Column(name = "settlement_type", nullable = false)
    private String settlementType; // INTEREST, EQUITY, DIVIDEND, PRINCIPAL
    
    // Settlement details
    @Column(name = "counterparty", nullable = false)
    private String counterparty;
    
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    // Status management
    @Column(name = "status", nullable = false)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
    
    @Column(name = "last_retry_date")
    private LocalDateTime lastRetryDate;
    
    @Column(name = "next_retry_date")
    private LocalDateTime nextRetryDate;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    // Completion details
    @Column(name = "actual_settlement_date")
    private LocalDate actualSettlementDate;
    
    @Column(name = "settlement_reference", length = 100)
    private String settlementReference;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    // Cancellation details
    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;
    
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
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
