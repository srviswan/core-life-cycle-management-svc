package com.financial.cashflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a cash flow record.
 */
@Entity
@Table(name = "cash_flows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CashFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cash_flow_id", nullable = false, length = 100)
    private String cashFlowId;

    @Column(name = "contract_id", nullable = false, length = 100)
    private String contractId;

    @Column(name = "lot_id", nullable = false, length = 100)
    private String lotId;

    @Column(name = "cash_flow_type", nullable = false, length = 50)
    private String cashFlowType; // INTEREST, DIVIDEND, PRINCIPAL, PNL

    @Column(name = "cash_flow_date", nullable = false)
    private LocalDate cashFlowDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // ACCRUAL, REALIZED_DEFERRED, REALIZED_UNSETTLED, REALIZED_SETTLED

    @Column(name = "calculation_basis", nullable = false, length = 50)
    private String calculationBasis; // DAILY_CLOSE, TRADE_LEVEL, SCHEDULED

    @Column(name = "accrual_start_date")
    private LocalDate accrualStartDate;

    @Column(name = "accrual_end_date")
    private LocalDate accrualEndDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "metadata", columnDefinition = "NVARCHAR(MAX)")
    private String metadata; // JSON string for additional calculation details

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "calculation_id", nullable = false, length = 100)
    private String calculationId;

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
