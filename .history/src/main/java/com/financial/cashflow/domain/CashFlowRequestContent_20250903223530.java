package com.financial.cashflow.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Main request model for cash flow calculations.
 * Contains all necessary data for self-contained cash flow processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowRequestContent implements Serializable {

    @NotBlank(message = "Request ID is required")
    @JsonProperty("requestId")
    private String requestId;

    @NotBlank(message = "Contract ID is required")
    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("calculationType")
    private String calculationType; // Optional - service will determine if not provided

    @NotNull(message = "Date range is required")
    @Valid
    @JsonProperty("dateRange")
    private DateRange dateRange;

    @NotEmpty(message = "At least one contract is required")
    @Valid
    @JsonProperty("contracts")
    private List<Contract> contracts;

    @NotEmpty(message = "At least one position is required")
    @Valid
    @JsonProperty("positions")
    private List<Position> positions;

    @NotEmpty(message = "At least one lot is required")
    @Valid
    @JsonProperty("lots")
    private List<Lot> lots;

    @Valid
    @JsonProperty("paymentSchedules")
    private List<PaymentSchedule> paymentSchedules;

    @Valid
    @JsonProperty("marketDataStrategy")
    private MarketDataStrategy marketDataStrategy;

    @Valid
    @JsonProperty("marketData")
    private MarketDataContainer marketData;

    /**
     * Date range for cash flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange implements Serializable {
        
        @NotNull(message = "From date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("fromDate")
        private LocalDate fromDate;

        @NotNull(message = "To date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("toDate")
        private LocalDate toDate;

        @NotBlank(message = "Calculation frequency is required")
        @JsonProperty("calculationFrequency")
        private String calculationFrequency; // DAILY, WEEKLY, MONTHLY
    }

    /**
     * Contract information for cash flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contract implements Serializable {
        
        @NotBlank(message = "Contract ID is required")
        @JsonProperty("contractId")
        private String contractId;

        @NotBlank(message = "Contract type is required")
        @JsonProperty("contractType")
        private String contractType; // SWAP, CFD, etc.

        @NotNull(message = "Notional amount is required")
        @JsonProperty("notionalAmount")
        private Double notionalAmount;

        @NotBlank(message = "Currency is required")
        @JsonProperty("currency")
        private String currency;

        @NotNull(message = "Start date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("startDate")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("endDate")
        private LocalDate endDate;

        @JsonProperty("interestRate")
        private Double interestRate;

        @JsonProperty("interestRateIndex")
        private String interestRateIndex;

        @JsonProperty("equityLeg")
        private EquityLeg equityLeg;

        @JsonProperty("interestLeg")
        private InterestLeg interestLeg;
    }

    /**
     * Equity leg details for synthetic swaps.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquityLeg implements Serializable {
        
        @NotBlank(message = "Underlier is required")
        @JsonProperty("underlier")
        private String underlier;

        @NotBlank(message = "Underlier type is required")
        @JsonProperty("underlierType")
        private String underlierType; // STOCK, INDEX, BASKET

        @JsonProperty("dividendTreatment")
        private String dividendTreatment; // REINVEST, PAY, NONE

        @JsonProperty("corporateActionTreatment")
        private String corporateActionTreatment; // ADJUST, IGNORE
    }

    /**
     * Interest leg details for synthetic swaps.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestLeg implements Serializable {
        
        @NotBlank(message = "Interest rate index is required")
        @JsonProperty("interestRateIndex")
        private String interestRateIndex;

        @NotNull(message = "Interest rate is required")
        @JsonProperty("interestRate")
        private Double interestRate;

        @NotBlank(message = "Payment frequency is required")
        @JsonProperty("paymentFrequency")
        private String paymentFrequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY

        @JsonProperty("dayCountConvention")
        private String dayCountConvention; // ACT/360, 30/360, etc.

        @JsonProperty("resetFrequency")
        private String resetFrequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY
    }

    /**
     * Position information for cash flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position implements Serializable {
        
        @NotBlank(message = "Position ID is required")
        @JsonProperty("positionId")
        private String positionId;

        @NotBlank(message = "Contract ID is required")
        @JsonProperty("contractId")
        private String contractId;

        @NotNull(message = "Quantity is required")
        @JsonProperty("quantity")
        private Double quantity;

        @NotNull(message = "Average price is required")
        @JsonProperty("averagePrice")
        private Double averagePrice;

        @NotBlank(message = "Currency is required")
        @JsonProperty("currency")
        private String currency;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("positionDate")
        private LocalDate positionDate;

        @JsonProperty("book")
        private String book;

        @JsonProperty("account")
        private String account;
    }

    /**
     * Lot information for detailed cash flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Lot implements Serializable {
        
        @NotBlank(message = "Lot ID is required")
        @JsonProperty("lotId")
        private String lotId;

        @NotBlank(message = "Position ID is required")
        @JsonProperty("positionId")
        private String positionId;

        @NotNull(message = "Quantity is required")
        @JsonProperty("quantity")
        private Double quantity;

        @NotNull(message = "Price is required")
        @JsonProperty("price")
        private Double price;

        @NotBlank(message = "Currency is required")
        @JsonProperty("currency")
        private String currency;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("tradeDate")
        private LocalDate tradeDate;

        @JsonProperty("settlementDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate settlementDate;

        @JsonProperty("costBasis")
        private Double costBasis;

        @JsonProperty("unrealizedPnL")
        private Double unrealizedPnL;
    }

    /**
     * Payment schedule for cash flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSchedule implements Serializable {
        
        @NotBlank(message = "Schedule ID is required")
        @JsonProperty("scheduleId")
        private String scheduleId;

        @NotBlank(message = "Contract ID is required")
        @JsonProperty("contractId")
        private String contractId;

        @NotBlank(message = "Payment type is required")
        @JsonProperty("paymentType")
        private String paymentType; // INTEREST, DIVIDEND, PRINCIPAL

        @NotNull(message = "Payment date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("paymentDate")
        private LocalDate paymentDate;

        @NotNull(message = "Payment amount is required")
        @JsonProperty("paymentAmount")
        private Double paymentAmount;

        @NotBlank(message = "Currency is required")
        @JsonProperty("currency")
        private String currency;

        @JsonProperty("paymentStatus")
        private String paymentStatus; // SCHEDULED, PAID, CANCELLED

        @JsonProperty("accrualStartDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate accrualStartDate;

        @JsonProperty("accrualEndDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate accrualEndDate;
    }

    /**
     * Market data strategy configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataStrategy implements Serializable {
        
        @NotBlank(message = "Strategy type is required")
        @JsonProperty("strategyType")
        private String strategyType; // HYBRID, EXTERNAL_ONLY, EMBEDDED_ONLY

        @JsonProperty("externalEndpoints")
        private List<String> externalEndpoints;

        @JsonProperty("fallbackStrategy")
        private String fallbackStrategy; // USE_EMBEDDED, FAIL, USE_DEFAULT

        @JsonProperty("cacheTimeout")
        private Integer cacheTimeout; // seconds

        @JsonProperty("retryAttempts")
        private Integer retryAttempts;
    }

    /**
     * Market data container with embedded data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataContainer implements Serializable {
        
        @JsonProperty("prices")
        private List<PriceData> prices;

        @JsonProperty("rates")
        private List<RateData> rates;

        @JsonProperty("fxRates")
        private List<FxRateData> fxRates;
    }

    /**
     * Price data for equity instruments.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceData implements Serializable {
        
        @NotBlank(message = "Instrument ID is required")
        @JsonProperty("instrumentId")
        private String instrumentId;

        @NotNull(message = "Price is required")
        @JsonProperty("price")
        private Double price;

        @NotBlank(message = "Currency is required")
        @JsonProperty("currency")
        private String currency;

        @NotNull(message = "Price date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("priceDate")
        private LocalDate priceDate;

        @JsonProperty("priceType")
        private String priceType; // CLOSE, OPEN, HIGH, LOW, VWAP
    }

    /**
     * Rate data for interest rate instruments.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateData implements Serializable {
        
        @NotBlank(message = "Rate index is required")
        @JsonProperty("rateIndex")
        private String rateIndex;

        @NotNull(message = "Rate is required")
        @JsonProperty("rate")
        private Double rate;

        @NotNull(message = "Rate date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("rateDate")
        private LocalDate rateDate;

        @JsonProperty("tenor")
        private String tenor; // 1M, 3M, 6M, 1Y, etc.
    }

    /**
     * FX rate data for currency conversions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FxRateData implements Serializable {
        
        @NotBlank(message = "From currency is required")
        @JsonProperty("fromCurrency")
        private String fromCurrency;

        @NotBlank(message = "To currency is required")
        @JsonProperty("toCurrency")
        private String toCurrency;

        @NotNull(message = "FX rate is required")
        @JsonProperty("fxRate")
        private Double fxRate;

        @NotNull(message = "Rate date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("rateDate")
        private LocalDate rateDate;
    }
}
