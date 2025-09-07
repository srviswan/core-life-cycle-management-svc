package com.financial.cashflow.controller;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.service.BatchProcessingService;
import com.financial.cashflow.service.CalculationEngine;
import com.financial.cashflow.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Cash Flow Controller with Enterprise Scaling Support
 * Routes requests between real-time and batch processing based on size
 */
@RestController
@RequestMapping("/api/v1/cashflows")
@RequiredArgsConstructor
@Slf4j
public class CashFlowController {
    
    private final CalculationEngine calculationEngine;
    private final MarketDataService marketDataService;
    private final BatchProcessingService batchProcessingService;
    
    /**
     * Calculate cash flows with automatic routing to batch or real-time processing
     */
    @PostMapping("/calculate")
    public CompletableFuture<ResponseEntity<CashFlowResponse>> calculateCashFlows(
            @RequestBody CashFlowRequest request) {
        
        log.info("Received cash flow calculation request: {} with {} contract positions", 
                request.getRequestId(), 
                request.getContractPositions() != null ? request.getContractPositions().size() : 0);
        
        try {
            // Determine processing strategy based on request size
            if (shouldUseBatchProcessing(request)) {
                log.info("Routing request {} to batch processing", request.getRequestId());
                return batchProcessingService.processBatch(request)
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(throwable -> {
                        log.error("Batch processing failed for request: {}", request.getRequestId(), throwable);
                        return ResponseEntity.internalServerError()
                            .body(CashFlowResponse.builder()
                                .requestId(request.getRequestId())
                                .status("ERROR")
                                .errorMessage("Batch processing failed: " + throwable.getMessage())
                                .build());
                    });
            } else {
                log.info("Routing request {} to real-time processing", request.getRequestId());
                return processRealTime(request);
            }
            
        } catch (Exception e) {
            log.error("Error processing request: {}", request.getRequestId(), e);
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError()
                    .body(CashFlowResponse.builder()
                        .requestId(request.getRequestId())
                        .status("ERROR")
                        .errorMessage("Processing failed: " + e.getMessage())
                        .build())
            );
        }
    }
    
    /**
     * Real-time processing for smaller requests
     */
    private CompletableFuture<ResponseEntity<CashFlowResponse>> processRealTime(CashFlowRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var marketData = marketDataService.loadMarketData(request);
                var response = calculationEngine.calculate(request, marketData);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Real-time processing failed for request: {}", request.getRequestId(), e);
                return ResponseEntity.internalServerError()
                    .body(CashFlowResponse.builder()
                        .requestId(request.getRequestId())
                        .status("ERROR")
                        .errorMessage("Real-time processing failed: " + e.getMessage())
                        .build());
            }
        });
    }
    
    /**
     * Determine if request should use batch processing
     */
    private boolean shouldUseBatchProcessing(CashFlowRequest request) {
        if (request.getContractPositions() == null || request.getContractPositions().isEmpty()) {
            return false;
        }
        
        // Count total positions
        int totalPositions = request.getContractPositions().stream()
            .mapToInt(cp -> cp.getPositions() != null ? cp.getPositions().size() : 0)
            .sum();
        
        // Count total lots
        int totalLots = request.getContractPositions().stream()
            .flatMap(cp -> cp.getPositions() != null ? cp.getPositions().stream() : java.util.stream.Stream.empty())
            .mapToInt(p -> p.getLots() != null ? p.getLots().size() : 0)
            .sum();
        
        // Use batch processing for large requests
        boolean useBatch = totalPositions > 1000 || totalLots > 5000;
        
        log.debug("Request {} analysis: {} positions, {} lots, useBatch={}", 
                request.getRequestId(), totalPositions, totalLots, useBatch);
        
        return useBatch;
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        try {
            // Check if services are healthy
            boolean calculationEngineHealthy = true; // Would implement actual health check
            boolean marketDataServiceHealthy = true; // Would implement actual health check
            boolean batchProcessingHealthy = true; // Would implement actual health check
            
            if (calculationEngineHealthy && marketDataServiceHealthy && batchProcessingHealthy) {
                return ResponseEntity.ok(HealthStatus.builder()
                    .status("UP")
                    .message("All services operational")
                    .build());
            } else {
                return ResponseEntity.status(503)
                    .body(HealthStatus.builder()
                        .status("DOWN")
                        .message("One or more services are down")
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(503)
                .body(HealthStatus.builder()
                    .status("DOWN")
                    .message("Health check failed: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Get processing statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ProcessingStats> getStats() {
        try {
            var batchStats = batchProcessingService.getStats();
            
            return ResponseEntity.ok(ProcessingStats.builder()
                .totalBatchesProcessed(batchStats.getTotalBatchesProcessed())
                .averageBatchSize(batchStats.getAverageBatchSize())
                .averageProcessingTime(batchStats.getAverageProcessingTime())
                .build());
        } catch (Exception e) {
            log.error("Error getting processing stats", e);
            return ResponseEntity.internalServerError()
                .body(ProcessingStats.builder()
                    .error("Failed to get stats: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Health status model
     */
    @lombok.Data
    @lombok.Builder
    public static class HealthStatus {
        private String status;
        private String message;
    }
    
    /**
     * Processing statistics model
     */
    @lombok.Data
    @lombok.Builder
    public static class ProcessingStats {
        private long totalBatchesProcessed;
        private double averageBatchSize;
        private double averageProcessingTime;
        private String error;
    }
}
