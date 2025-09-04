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
 * Entity representing a settlement instruction.
 */
@Entity
@Table(name = "settlement_instructions", schema = "cashflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SettlementInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "settlement_id", nullable = false, length = 100)
    private String settlementId;

    @Column(name = "contract_id", nullable = false, length = 100)
    private String contractId;

    @Column(name = "cash_flow_id", nullable = false, length = 100)
    private String cashFlowId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "settlement_type", nullable = false, length = 50)
    private String settlementType; // INTEREST_PAYMENT, DIVIDEND_PAYMENT, PNL_SETTLEMENT, PRINCIPAL_PAYMENT

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, PROCESSING, SETTLED, FAILED

    @Column(name = "priority", nullable = false, length = 20)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(name = "settlement_method", nullable = false, length = 50)
    private String settlementMethod; // WIRE_TRANSFER, ACH, etc.

    @Column(name = "reference_number", nullable = false, length = 100)
    private String referenceNumber;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "routing_number", length = 50)
    private String routingNumber;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(name = "iban", length = 50)
    private String iban;

    @Column(name = "account_from", length = 100)
    private String accountFrom;

    @Column(name = "account_to", length = 100)
    private String accountTo;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

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
