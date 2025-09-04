package com.financial.cashflow.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response model for cash flow calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowResponse {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("calculationId")
    private String calculationId;

    @JsonProperty("calculationType")
    private CalculationType calculationType;

    @JsonProperty("calculationDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculationDate;

    @JsonProperty("dateRange")
    private CashFlowRequestContent.DateRange dateRange;

    @JsonProperty("cashFlows")
    private List<CashFlow> cashFlows;

    @JsonProperty("settlementInstructions")
    private List<SettlementInstruction> settlementInstructions;

    @JsonProperty("summary")
    private CalculationSummary summary;

    @JsonProperty("status")
    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED

    @JsonProperty("errors")
    private List<CalculationError> errors;

    /**
     * Calculation type determined by the service.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationType {
        
        @JsonProperty("determinedType")
        private String determinedType; // HISTORICAL_RECALCULATION, REAL_TIME_PROCESSING, INCREMENTAL_UPDATE
        
        @JsonProperty("reason")
        private String reason; // Explanation of why this type was chosen
        
        @JsonProperty("confidence")
        private Double confidence; // 0.0 to 1.0 confidence level
    }

    /**
     * Individual cash flow entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlow {
        
        @JsonProperty("cashFlowId")
        private String cashFlowId;

        @JsonProperty("contractId")
        private String contractId;

        @JsonProperty("lotId")
        private String lotId;

        @JsonProperty("cashFlowType")
        private String cashFlowType; // INTEREST, DIVIDEND, PRINCIPAL, PNL

        @JsonProperty("cashFlowDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate cashFlowDate;

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("status")
        private String status; // ACCRUAL, REALIZED_DEFERRED, REALIZED_UNSETTLED, REALIZED_SETTLED

        @JsonProperty("calculationBasis")
        private String calculationBasis; // DAILY_CLOSE, TRADE_LEVEL, SCHEDULED

        @JsonProperty("accrualStartDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate accrualStartDate;

        @JsonProperty("accrualEndDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate accrualEndDate;

        @JsonProperty("settlementDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate settlementDate;

        @JsonProperty("metadata")
        private Object metadata; // Additional calculation details
    }

    /**
     * Settlement instruction for cash flows.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementInstruction {
        
        @JsonProperty("settlementId")
        private String settlementId;

        @JsonProperty("contractId")
        private String contractId;

        @JsonProperty("cashFlowId")
        private String cashFlowId;

        @JsonProperty("settlementDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate settlementDate;

        @JsonProperty("settlementType")
        private String settlementType; // INTEREST, DIVIDEND, PRINCIPAL, PNL

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("status")
        private String status; // PENDING, PROCESSING, SETTLED, FAILED

        @JsonProperty("instructionDetails")
        private SettlementDetails instructionDetails;
    }

    /**
     * Settlement instruction details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementDetails {
        
        @JsonProperty("accountFrom")
        private String accountFrom;

        @JsonProperty("accountTo")
        private String accountTo;

        @JsonProperty("bankDetails")
        private BankDetails bankDetails;

        @JsonProperty("reference")
        private String reference;

        @JsonProperty("priority")
        private String priority; // HIGH, NORMAL, LOW
    }

    /**
     * Bank details for settlement.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankDetails {
        
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
    }

    /**
     * Summary of cash flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationSummary {
        
        @JsonProperty("totalCashFlows")
        private Integer totalCashFlows;

        @JsonProperty("totalAmount")
        private Double totalAmount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("interestAmount")
        private Double interestAmount;

        @JsonProperty("dividendAmount")
        private Double dividendAmount;

        @JsonProperty("principalAmount")
        private Double principalAmount;

        @JsonProperty("pnlAmount")
        private Double pnlAmount;

        @JsonProperty("settlementInstructions")
        private Integer settlementInstructions;

        @JsonProperty("calculationDuration")
        private Long calculationDuration; // milliseconds

        @JsonProperty("cacheHit")
        private Boolean cacheHit;
    }

    /**
     * Calculation error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationError {
        
        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;

        @JsonProperty("severity")
        private String severity; // ERROR, WARNING, INFO

        @JsonProperty("affectedComponent")
        private String affectedComponent; // CONTRACT, POSITION, LOT, MARKET_DATA

        @JsonProperty("suggestion")
        private String suggestion; // Suggested fix or workaround
    }
}
