package com.financial.cashflow.service;

import com.financial.cashflow.model.CalculationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing calculation status
 */
@Service
@Slf4j
public class CalculationStatusService {
    
    private final ConcurrentHashMap<String, CalculationStatus> statusMap = new ConcurrentHashMap<>();
    
    /**
     * Update calculation status
     */
    public void updateStatus(String statusId, String status, Integer progressPercentage) {
        updateStatus(statusId, status, progressPercentage, null);
    }
    
    /**
     * Update calculation status with error message
     */
    public void updateStatus(String statusId, String status, Integer progressPercentage, String errorMessage) {
        CalculationStatus currentStatus = statusMap.get(statusId);
        
        CalculationStatus updatedStatus = CalculationStatus.builder()
            .requestId(currentStatus != null ? currentStatus.getRequestId() : null)
            .statusId(statusId)
            .status(status)
            .progressPercentage(progressPercentage)
            .statusUrl("/api/v1/cashflows/status/" + statusId)
            .errorMessage(errorMessage)
            .startTime(currentStatus != null ? currentStatus.getStartTime() : LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .completedTime("COMPLETED".equals(status) || "FAILED".equals(status) ? LocalDateTime.now() : null)
            .build();
        
        statusMap.put(statusId, updatedStatus);
        log.debug("Updated status for {}: {} ({}%)", statusId, status, progressPercentage);
    }
    
    /**
     * Get calculation status
     */
    public CalculationStatus getStatus(String statusId) {
        CalculationStatus status = statusMap.get(statusId);
        if (status == null) {
            throw new IllegalArgumentException("Status not found for ID: " + statusId);
        }
        return status;
    }
    
    /**
     * Create initial status
     */
    public void createStatus(String statusId, String requestId) {
        CalculationStatus status = CalculationStatus.builder()
            .requestId(requestId)
            .statusId(statusId)
            .status("PENDING")
            .progressPercentage(0)
            .statusUrl("/api/v1/cashflows/status/" + statusId)
            .startTime(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();
        
        statusMap.put(statusId, status);
        log.debug("Created status for {}: {}", statusId, requestId);
    }
    
    /**
     * Remove status
     */
    public void removeStatus(String statusId) {
        statusMap.remove(statusId);
        log.debug("Removed status for {}", statusId);
    }
    
    /**
     * Get all statuses
     */
    public java.util.Collection<CalculationStatus> getAllStatuses() {
        return statusMap.values();
    }
    
    /**
     * Get calculation status by natural key
     */
    public CalculationStatus getStatusByNaturalKey(String contractId, java.time.LocalDate fromDate, 
                                                  java.time.LocalDate toDate, String calculationType) {
        // In a real implementation, you would query the database for the status
        // For now, we'll search through the in-memory map
        for (CalculationStatus status : statusMap.values()) {
            if (status.getRequestId() != null && status.getRequestId().contains(contractId)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status not found for natural key");
    }
}
