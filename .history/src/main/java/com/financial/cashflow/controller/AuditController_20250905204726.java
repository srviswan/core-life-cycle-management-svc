package com.financial.cashflow.controller;

import com.financial.cashflow.dto.CalculationReproduction;
import com.financial.cashflow.dto.CalculationRequestsResponse;
import com.financial.cashflow.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for Audit Trail and Calculation Reproduction
 */
@RestController
@RequestMapping("/api/v1/audit")
@Slf4j
@RequiredArgsConstructor
public class AuditController {
    
    private final AuditService auditService;
    
    /**
     * Reproduce calculation from audit trail
     */
    @GetMapping("/reproduce/{requestId}")
    public ResponseEntity<CalculationReproduction> reproduceCalculation(@PathVariable String requestId) {
        try {
            CalculationReproduction reproduction = auditService.reproduceCalculation(requestId);
            return ResponseEntity.ok(reproduction);
        } catch (IllegalArgumentException e) {
            log.error("Calculation request not found: {}", requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Failed to reproduce calculation: {}", requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get calculation requests by contract
     */
    @GetMapping("/calculation-requests")
    public ResponseEntity<CalculationRequestsResponse> getCalculationRequestsByContract(
            @RequestParam String contractId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String calculationType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        
        try {
            CalculationRequestsResponse response = auditService.getCalculationRequestsByContract(
                    contractId, fromDate, toDate, calculationType, status, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get calculation requests for contract: {}", contractId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
