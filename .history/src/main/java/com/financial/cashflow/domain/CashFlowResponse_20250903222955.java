package com.financial.cashflow.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Response model for cash flow calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowResponse implements Serializable {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("calculationId")
    private String calculationId;

    @JsonProperty("calculationType")
    private String calculationType;

    @JsonProperty("cashFlows")
    private List<CashFlow> cashFlows;

    @JsonProperty("settlementInstructions")
    private List<SettlementInstruction> settlementInstructions;

    @JsonProperty("totalAmount")
    private Double totalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("calculationDuration")
    private Long calculationDuration;

    @JsonProperty("status")
    private String status;

    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Individual cash flow record.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlow implements Serializable {
        
        @JsonProperty("cashFlowId")
        private String cashFlowId;

        @JsonProperty("contractId")
        private String contractId;

        @JsonProperty("lotId")
        private String lotId;

        @JsonProperty("cashFlowType")
        private String cashFlowType; // INTEREST, DIVIDEND, PRINCIPAL, PNL

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("cashFlowDate")
        private LocalDate cashFlowDate;

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("status")
        private String status; // ACCRUAL, REALIZED_UNSETTLED, REALIZED_SETTLED

        @JsonProperty("calculationBasis")
        private String calculationBasis; // DAILY_CLOSE, PERIOD_BASED, SCHEDULED, etc.

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("accrualStartDate")
        private LocalDate accrualStartDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("accrualEndDate")
        private LocalDate accrualEndDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("settlementDate")
        private LocalDate settlementDate;

        @JsonProperty("metadata")
        private String metadata; // JSON string for additional data
    }

    /**
     * Settlement instruction for cash flow payments.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementInstruction implements Serializable {
        
        @JsonProperty("settlementId")
        private String settlementId;

        @JsonProperty("contractId")
        private String contractId;

        @JsonProperty("cashFlowId")
        private String cashFlowId;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("settlementDate")
        private LocalDate settlementDate;

        @JsonProperty("settlementType")
        private String settlementType; // INTEREST, DIVIDEND, PRINCIPAL, PNL

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("status")
        private String status; // PENDING, PROCESSING, COMPLETED, FAILED

        @JsonProperty("priority")
        private Integer priority;

        @JsonProperty("settlementMethod")
        private String settlementMethod; // WIRE, ACH, SWIFT, etc.

        @JsonProperty("referenceNumber")
        private String referenceNumber;

        @JsonProperty("bankName")
        private String bankName;

        @JsonProperty("accountNumber")
        private String accountNumber;

        @JsonProperty("routingNumber")
        private String routingNumber;

        @JsonProperty("swiftCode")
        private String swiftCode;

        @JsonProperty("iban")
        private String iban;

        @JsonProperty("accountFrom")
        private String accountFrom;

        @JsonProperty("accountTo")
        private String accountTo;

        @JsonProperty("metadata")
        private String metadata; // JSON string for additional data
    }
}
