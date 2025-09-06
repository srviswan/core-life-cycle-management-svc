package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request model for cash flow calculations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowRequest {
    
    private String requestId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate calculationDate;
    
    private DateRange dateRange;
    
    private CalculationType calculationType;
    
    private MarketDataStrategy marketDataStrategy;
    
    private MarketDataContainer marketData;
    
    private List<Contract> contracts;
    
    private List<Lot> lots;
    
    private String underlying;
    
    private String index;
    
    private String cacheKey;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fromDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate toDate;
    }
    
    public enum CalculationType {
        REAL_TIME_PROCESSING,
        HISTORICAL_RECALCULATION,
        BATCH_PROCESSING
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataStrategy {
        private MarketDataMode mode;
        private String cacheKey;
        private Integer timeoutSeconds;
        
        public enum MarketDataMode {
            HYBRID,
            SELF_CONTAINED,
            ENDPOINTS
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataContainer {
        private MarketDataContent data;
        private String version;
        private LocalDate asOfDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataContent {
        private List<SecurityData> securities;
        private List<RateData> rates;
        private List<DividendData> dividends;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityData {
        private String symbol;
        private Double basePrice;
        private LocalDate baseDate;
        private List<PriceChange> changes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceChange {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Double price;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateData {
        private String index;
        private Double baseRate;
        private LocalDate baseDate;
        private List<RateChange> changes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateChange {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Double rate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DividendData {
        private String symbol;
        private List<Dividend> dividends;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dividend {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate exDate;
        private Double amount;
        private String currency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contract {
        private String contractId;
        private String underlying;
        private String positionId;
        private String lotId;
        private String scheduleId;
        private ContractType type;
        private Double notionalAmount;
        private String currency;
        private LocalDate startDate;
        private LocalDate endDate;
        private String index;
        
        public enum ContractType {
            EQUITY_SWAP,
            EQUITY_FORWARD,
            EQUITY_OPTION,
            INTEREST_RATE_SWAP,
            BOND
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Lot {
        private String lotId;
        private String contractId;
        private String positionId;
        private String underlying;
        private Double quantity;
        private Double costPrice;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate costDate;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate settlementDate;
        private LotType lotType;
        private LotStatus status;
        private String unwindingMethod;
        
        public enum LotType {
            NEW_LOT,
            ADJUSTMENT_LOT,
            CLOSING_LOT
        }
        
        public enum LotStatus {
            ACTIVE,
            CLOSED,
            ADJUSTED
        }
    }
}
