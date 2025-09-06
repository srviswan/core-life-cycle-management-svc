package com.financial.cashflow.controller;

import com.financial.cashflow.dto.*;
import com.financial.cashflow.model.SettlementInstruction;
import com.financial.cashflow.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for Settlement Management
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class SettlementController {
    
    private final SettlementService settlementService;
    
    /**
     * Get all settlements with filtering and pagination
     */
    @GetMapping("/settlements")
    public ResponseEntity<SettlementResponse> getAllSettlements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String settlementType,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        
        try {
            SettlementResponse response = settlementService.getAllSettlements(
                    status, counterparty, currency, settlementType, fromDate, toDate, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get settlements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create settlement instruction
     */
    @PostMapping("/settlements")
    public ResponseEntity<SettlementInstruction> createSettlement(@RequestBody SettlementCreateRequest request) {
        try {
            SettlementInstruction settlement = settlementService.createSettlement(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(settlement);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create settlement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Failed to create settlement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get pending settlements
     */
    @GetMapping("/settlements/pending")
    public ResponseEntity<SettlementResponse> getPendingSettlements(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String settlementType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        
        try {
            SettlementResponse response = settlementService.getPendingSettlements(
                    counterparty, currency, settlementType, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get pending settlements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get settlement by ID
     */
    @GetMapping("/settlements/{settlementId}")
    public ResponseEntity<SettlementInstruction> getSettlementById(@PathVariable String settlementId) {
        try {
            SettlementInstruction settlement = settlementService.getSettlementById(settlementId);
            return ResponseEntity.ok(settlement);
        } catch (IllegalArgumentException e) {
            log.error("Settlement not found: {}", settlementId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Failed to get settlement: {}", settlementId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get settlement by natural key
     */
    @GetMapping("/settlements/natural-key")
    public ResponseEntity<SettlementInstruction> getSettlementByNaturalKey(
            @RequestParam String contractId,
            @RequestParam String cashFlowId,
            @RequestParam LocalDate settlementDate,
            @RequestParam String settlementType) {
        
        try {
            SettlementInstruction settlement = settlementService.getSettlementByNaturalKey(
                    contractId, cashFlowId, settlementDate, settlementType);
            return ResponseEntity.ok(settlement);
        } catch (IllegalArgumentException e) {
            log.error("Settlement not found for natural key");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Failed to get settlement by natural key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update settlement status
     */
    @PutMapping("/settlements/{settlementId}/status")
    public ResponseEntity<SettlementInstruction> updateSettlementStatus(
            @PathVariable String settlementId,
            @RequestBody SettlementStatusUpdate update) {
        
        try {
            SettlementInstruction settlement = settlementService.updateSettlementStatus(settlementId, update);
            return ResponseEntity.ok(settlement);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Failed to update settlement status: {}", settlementId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Bulk update settlement status
     */
    @PutMapping("/settlements/bulk/status")
    public ResponseEntity<BulkSettlementStatusResponse> bulkUpdateSettlementStatus(
            @RequestBody BulkSettlementStatusUpdate update) {
        
        try {
            BulkSettlementStatusResponse response = settlementService.bulkUpdateSettlementStatus(update);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to bulk update settlement status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get settlement summary
     */
    @GetMapping("/settlements/summary")
    public ResponseEntity<SettlementResponse.SettlementSummary> getSettlementSummary(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String currency) {
        
        try {
            SettlementResponse.SettlementSummary summary = settlementService.getSettlementSummary(
                    fromDate, toDate, counterparty, currency);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to get settlement summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
