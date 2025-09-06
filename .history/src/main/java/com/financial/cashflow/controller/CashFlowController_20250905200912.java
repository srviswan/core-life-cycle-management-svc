package com.financial.cashflow.controller;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.model.CalculationStatus;
import com.financial.cashflow.model.SettlementInstruction;
import com.financial.cashflow.service.CashFlowService;
import com.financial.cashflow.service.CalculationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CashFlowResponse.error(e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CashFlowResponse.error(e.getMessage()));
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
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Cash Flow Management Service is running");
    }
}
