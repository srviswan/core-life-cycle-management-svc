package com.financial.cashflow.controller;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.model.CalculationStatus;
import com.financial.cashflow.model.SettlementInstruction;
import com.financial.cashflow.monitoring.CashFlowHealthIndicator;
import com.financial.cashflow.service.CashFlowService;
import com.financial.cashflow.service.CalculationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Cash Flow Management Service
 * Following conventional Spring MVC pattern with synchronous APIs
 */
@RestController
@RequestMapping("/api/v1/cashflows")
@Slf4j
@RequiredArgsConstructor
public class CashFlowController {
    
    private final CashFlowService cashFlowService;
    private final CalculationStatusService statusService;
    private final CashFlowHealthIndicator healthIndicator;
    
    /**
     * Calculate cash flows synchronously
     */
    @PostMapping("/calculate")
    public ResponseEntity<CashFlowResponse> calculateCashFlows(@RequestBody CashFlowRequest request) {
        log.info("Received cash flow calculation request: {}", request.getRequestId());
        
        try {
            CashFlowResponse response = cashFlowService.calculate(request);
            log.info("Calculation completed for request: {}", request.getRequestId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Calculation failed for request: {}", request.getRequestId(), e);
            throw e; // Let exceptions propagate to GlobalExceptionHandler
        }
    }
    
    /**
     * Calculate cash flows in real-time (optimized for speed)
     */
    @PostMapping("/calculate/real-time")
    public ResponseEntity<CashFlowResponse> calculateRealTime(@RequestBody CashFlowRequest request) {
        log.info("Received real-time calculation request: {}", request.getRequestId());
        
        try {
            request.setCalculationType(CashFlowRequest.CalculationType.REAL_TIME_PROCESSING);
            CashFlowResponse response = cashFlowService.calculateRealTime(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Real-time calculation failed for request: {}", request.getRequestId(), e);
            throw e; // Let exceptions propagate to GlobalExceptionHandler
        }
    }
    
    /**
     * Start historical calculation asynchronously
     */
    @PostMapping("/calculate/historical")
    public ResponseEntity<CalculationStatus> calculateHistorical(@RequestBody CashFlowRequest request) {
        log.info("Received historical calculation request: {}", request.getRequestId());
        
        try {
            request.setCalculationType(CashFlowRequest.CalculationType.HISTORICAL_RECALCULATION);
            String statusId = cashFlowService.calculateHistoricalAsync(request);
            return ResponseEntity.accepted()
                .body(CalculationStatus.builder()
                    .requestId(request.getRequestId())
                    .statusId(statusId)
                    .status("PROCESSING")
                    .statusUrl("/api/v1/cashflows/status/" + statusId)
                    .build());
        } catch (Exception e) {
            log.error("Historical calculation failed for request: {}", request.getRequestId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CalculationStatus.error(e.getMessage()));
        }
    }
    
    /**
     * Get calculation status
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<CalculationStatus> getCalculationStatus(@PathVariable String requestId) {
        try {
            CalculationStatus status = statusService.getStatus(requestId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get status for request: {}", requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CalculationStatus.error(e.getMessage()));
        }
    }
    
    /**
     * Get calculation status by natural key
     */
    @GetMapping("/status/natural-key")
    public ResponseEntity<CalculationStatus> getCalculationStatusByNaturalKey(
            @RequestParam String contractId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam String calculationType) {
        
        try {
            CalculationStatus status = statusService.getStatusByNaturalKey(
                    contractId, fromDate, toDate, calculationType);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get status by natural key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CalculationStatus.error(e.getMessage()));
        }
    }
    
    /**
     * Get cash flows by contract
     */
    @GetMapping("/cashflows/{contractId}")
    public ResponseEntity<List<CashFlowResponse.CashFlow>> getCashFlowsByContract(
            @PathVariable String contractId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) String cashFlowType,
            @RequestParam(required = false) String state) {
        
        try {
            List<CashFlowResponse.CashFlow> cashFlows = cashFlowService.getCashFlowsByContract(
                contractId, fromDate, toDate, cashFlowType, state);
            return ResponseEntity.ok(cashFlows);
        } catch (Exception e) {
            log.error("Failed to get cash flows for contract: {}", contractId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get pending settlements
     */
    @GetMapping("/settlements/pending")
    public ResponseEntity<List<SettlementInstruction>> getPendingSettlements(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String currency) {
        
        try {
            List<SettlementInstruction> settlements = cashFlowService.getPendingSettlements(counterparty, currency);
            return ResponseEntity.ok(settlements);
        } catch (Exception e) {
            log.error("Failed to get pending settlements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // =====================================================
    // CONSOLIDATION ENDPOINTS - LOT TO POSITION TO CONTRACT
    // =====================================================
    
    /**
     * Get cash flows consolidated by lot (most granular level)
     */
    @GetMapping("/cashflows/lot/{lotId}")
    public ResponseEntity<List<CashFlowResponse.CashFlow>> getCashFlowsByLot(
            @PathVariable String lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            List<CashFlowResponse.CashFlow> cashFlows = cashFlowService.getCashFlowsByLot(lotId, fromDate, toDate);
            return ResponseEntity.ok(cashFlows);
        } catch (Exception e) {
            log.error("Failed to get cash flows for lot: {}", lotId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get cash flows consolidated by position
     */
    @GetMapping("/cashflows/position/{positionId}")
    public ResponseEntity<List<CashFlowResponse.CashFlow>> getCashFlowsByPosition(
            @PathVariable String positionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            List<CashFlowResponse.CashFlow> cashFlows = cashFlowService.getCashFlowsByPosition(positionId, fromDate, toDate);
            return ResponseEntity.ok(cashFlows);
        } catch (Exception e) {
            log.error("Failed to get cash flows for position: {}", positionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get aggregated cash flows by position (sum of all lots in position)
     */
    @GetMapping("/cashflows/position/{positionId}/aggregated")
    public ResponseEntity<com.financial.cashflow.model.CashFlowAggregation> getAggregatedCashFlowsByPosition(
            @PathVariable String positionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            com.financial.cashflow.model.CashFlowAggregation aggregation = 
                cashFlowService.getAggregatedCashFlowsByPosition(positionId, fromDate, toDate);
            return ResponseEntity.ok(aggregation);
        } catch (Exception e) {
            log.error("Failed to get aggregated cash flows for position: {}", positionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get aggregated cash flows by contract (sum of all positions in contract)
     */
    @GetMapping("/cashflows/contract/{contractId}/aggregated")
    public ResponseEntity<com.financial.cashflow.model.CashFlowAggregation> getAggregatedCashFlowsByContract(
            @PathVariable String contractId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            com.financial.cashflow.model.CashFlowAggregation aggregation = 
                cashFlowService.getAggregatedCashFlowsByContract(contractId, fromDate, toDate);
            return ResponseEntity.ok(aggregation);
        } catch (Exception e) {
            log.error("Failed to get aggregated cash flows for contract: {}", contractId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get hierarchical cash flow breakdown (contract -> position -> lot)
     */
    @GetMapping("/cashflows/contract/{contractId}/hierarchy")
    public ResponseEntity<List<com.financial.cashflow.model.CashFlowHierarchy>> getCashFlowHierarchy(
            @PathVariable String contractId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            List<com.financial.cashflow.model.CashFlowHierarchy> hierarchy = 
                cashFlowService.getCashFlowHierarchy(contractId, fromDate, toDate);
            return ResponseEntity.ok(hierarchy);
        } catch (Exception e) {
            log.error("Failed to get cash flow hierarchy for contract: {}", contractId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        String status = healthIndicator.getHealthStatus();
        HttpStatus httpStatus = healthIndicator.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(status);
    }
}
