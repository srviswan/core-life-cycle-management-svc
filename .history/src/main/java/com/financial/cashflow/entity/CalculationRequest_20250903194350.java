package com.financial.cashflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a calculation request for audit purposes.
 */
@Entity
@Table(name = "calculation_requests", schema = "cashflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CalculationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "contract_id", nullable = false, length = 100)
    private String contractId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "calculation_type", nullable = false, length = 50)
    private String calculationType; // HISTORICAL_RECALCULATION, REAL_TIME_PROCESSING, INCREMENTAL_UPDATE

    @Column(name = "calculation_id", nullable = false, length = 100)
    private String calculationId;

    @Column(name = "input_data_hash", nullable = false, length = 64)
    private String inputDataHash; // SHA-256 hash of input data for deduplication

    @Column(name = "input_data_snapshot", columnDefinition = "NVARCHAR(MAX)")
    private String inputDataSnapshot; // Optional JSON snapshot of input data

    @Column(name = "status", nullable = false, length = 50)
    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED

    @Column(name = "cash_flow_count")
    private Integer cashFlowCount;

    @Column(name = "settlement_count")
    private Integer settlementCount;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private java.math.BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "calculation_duration")
    private Long calculationDuration; // milliseconds

    @Column(name = "error_message", columnDefinition = "NVARCHAR(MAX)")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;
}
