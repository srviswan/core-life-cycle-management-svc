package com.financial.cashflow.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for storing calculation request audit trail
 */
@Entity
@Table(name = "calculation_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationRequestEntity {
    
    @Id
    @Column(name = "request_id")
    private String requestId;
    
    @Column(name = "contract_id", nullable = false)
    private String contractId;
    
    @Column(name = "calculation_type", nullable = false)
    private String calculationType;
    
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;
    
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "progress_percentage")
    private Integer progressPercentage;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "input_data_hash", length = 64)
    private String inputDataHash;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
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
}
