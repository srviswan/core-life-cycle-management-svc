package com.financial.cashflow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Status model for tracking calculation progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationStatus {
    
    private String requestId;
    
    private String statusId;
    
    private String status;
    
    private Integer progressPercentage;
    
    private String statusUrl;
    
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedTime;
    
    private Long estimatedTimeRemainingMs;
    
    private String resultUrl;
    
    public static CalculationStatus error(String errorMessage) {
        return CalculationStatus.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
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
}
