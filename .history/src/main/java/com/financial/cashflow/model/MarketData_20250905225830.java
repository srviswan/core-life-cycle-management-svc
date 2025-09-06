package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Market data model for calculations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    
    private PriceData price;
    
    private RateData rate;
    
    private DividendData dividends;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String source;
    
    private Boolean isValid;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validUntil;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceData {
        private String symbol;
        private Double basePrice;
        private LocalDate baseDate;
        private List<PriceChange> changes;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PriceChange {
            @JsonFormat(pattern = "yyyy-MM-dd")
            private LocalDate date;
            private Double price;
        }
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
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RateChange {
            @JsonFormat(pattern = "yyyy-MM-dd")
            private LocalDate date;
            private Double rate;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DividendData {
        private String symbol;
        private List<Dividend> dividends;
    }
}
