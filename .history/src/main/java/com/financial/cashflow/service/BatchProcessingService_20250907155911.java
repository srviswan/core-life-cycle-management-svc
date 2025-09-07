package com.financial.cashflow.service;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Batch Processing Service for Large-Scale Cash Flow Calculations
 * Handles requests that exceed the real-time processing threshold
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchProcessingService {
    
    private final CalculationEngine calculationEngine;
    private final MarketDataService marketDataService;
    
    @Qualifier("batchProcessingExecutor")
    private final Executor batchExecutor;
    
    // Threshold for batch processing (configurable)
    private static final int BATCH_THRESHOLD = 1000; // positions
    
    /**
     * Process large requests using batch processing
     */
    public CompletableFuture<CashFlowResponse> processBatch(CashFlowRequest request) {
        log.info("Starting batch processing for request: {} with {} contract positions", 
                request.getRequestId(), request.getContractPositions().size());
        
        // Check if request qualifies for batch processing
        if (!shouldUseBatchProcessing(request)) {
            log.info("Request {} does not qualify for batch processing, using real-time processing", 
                    request.getRequestId());
            return CompletableFuture.completedFuture(
                calculationEngine.calculate(request, marketDataService.loadMarketData(request))
            );
        }
        
        // Split request into manageable chunks
        List<CashFlowRequest> chunks = splitRequestIntoChunks(request);
        log.info("Split request {} into {} chunks", request.getRequestId(), chunks.size());
        
        // Process chunks in parallel
        List<CompletableFuture<CashFlowResponse>> chunkFutures = chunks.stream()
            .map(chunk -> processChunkAsync(chunk))
            .collect(Collectors.toList());
        
        // Wait for all chunks to complete and aggregate results
        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> aggregateResults(chunkFutures, request));
    }
    
    /**
     * Determine if request should use batch processing
     */
    private boolean shouldUseBatchProcessing(CashFlowRequest request) {
        if (request.getContractPositions() == null) {
            return false;
        }
        
        int totalPositions = request.getContractPositions().stream()
            .mapToInt(cp -> cp.getPositions() != null ? cp.getPositions().size() : 0)
            .sum();
        
        return totalPositions > BATCH_THRESHOLD;
    }
    
    /**
     * Split large request into smaller chunks
     */
    private List<CashFlowRequest> splitRequestIntoChunks(CashFlowRequest originalRequest) {
        List<CashFlowRequest> chunks = new ArrayList<>();
        
        // Group contracts by size to create balanced chunks
        List<List<CashFlowRequest.ContractPosition>> contractGroups = 
            groupContractsIntoChunks(originalRequest.getContractPositions());
        
        for (int i = 0; i < contractGroups.size(); i++) {
            List<CashFlowRequest.ContractPosition> group = contractGroups.get(i);
            
            CashFlowRequest chunk = CashFlowRequest.builder()
                .requestId(originalRequest.getRequestId() + "-chunk-" + i)
                .calculationDate(originalRequest.getCalculationDate())
                .dateRange(originalRequest.getDateRange())
                .calculationType(originalRequest.getCalculationType())
                .marketDataStrategy(originalRequest.getMarketDataStrategy())
                .marketData(originalRequest.getMarketData())
                .contractPositions(group)
                .build();
            
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    /**
     * Group contracts into balanced chunks
     */
    private List<List<CashFlowRequest.ContractPosition>> groupContractsIntoChunks(
            List<CashFlowRequest.ContractPosition> contractPositions) {
        
        List<List<CashFlowRequest.ContractPosition>> groups = new ArrayList<>();
        List<CashFlowRequest.ContractPosition> currentGroup = new ArrayList<>();
        int currentGroupSize = 0;
        int maxChunkSize = 500; // Maximum positions per chunk
        
        for (CashFlowRequest.ContractPosition contractPosition : contractPositions) {
            int contractSize = contractPosition.getPositions() != null ? 
                contractPosition.getPositions().size() : 0;
            
            // If adding this contract would exceed chunk size, start new group
            if (currentGroupSize + contractSize > maxChunkSize && !currentGroup.isEmpty()) {
                groups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
                currentGroupSize = 0;
            }
            
            currentGroup.add(contractPosition);
            currentGroupSize += contractSize;
        }
        
        // Add the last group if it's not empty
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        
        return groups;
    }
    
    /**
     * Process a single chunk asynchronously
     */
    private CompletableFuture<CashFlowResponse> processChunkAsync(CashFlowRequest chunk) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Processing chunk: {}", chunk.getRequestId());
                
                // Load market data for this chunk
                var marketData = marketDataService.loadMarketData(chunk);
                
                // Calculate cash flows for this chunk
                var response = calculationEngine.calculate(chunk, marketData);
                
                log.debug("Completed chunk: {} with {} contract results", 
                        chunk.getRequestId(), response.getContractResults().size());
                
                return response;
                
            } catch (Exception e) {
                log.error("Error processing chunk: {}", chunk.getRequestId(), e);
                throw new RuntimeException("Failed to process chunk: " + chunk.getRequestId(), e);
            }
        }, batchExecutor);
    }
    
    /**
     * Aggregate results from all chunks
     */
    private CashFlowResponse aggregateResults(List<CompletableFuture<CashFlowResponse>> chunkFutures, 
                                            CashFlowRequest originalRequest) {
        log.info("Aggregating results from {} chunks", chunkFutures.size());
        
        // Collect all contract results
        List<CashFlowResponse.ContractResult> allContractResults = new ArrayList<>();
        
        for (CompletableFuture<CashFlowResponse> future : chunkFutures) {
            try {
                CashFlowResponse chunkResponse = future.get();
                allContractResults.addAll(chunkResponse.getContractResults());
            } catch (Exception e) {
                log.error("Error getting chunk result", e);
                throw new RuntimeException("Failed to aggregate chunk results", e);
            }
        }
        
        // Calculate aggregated totals
        double totalPnl = allContractResults.stream()
            .mapToDouble(cr -> cr.getTotalPnl() != null ? cr.getTotalPnl().doubleValue() : 0.0)
            .sum();
        
        double totalInterest = allContractResults.stream()
            .mapToDouble(cr -> cr.getTotalInterest() != null ? cr.getTotalInterest().doubleValue() : 0.0)
            .sum();
        
        double totalDividends = allContractResults.stream()
            .mapToDouble(cr -> cr.getTotalDividends() != null ? cr.getTotalDividends().doubleValue() : 0.0)
            .sum();
        
        double totalCashFlows = allContractResults.stream()
            .mapToDouble(cr -> cr.getTotalCashFlows() != null ? cr.getTotalCashFlows().doubleValue() : 0.0)
            .sum();
        
        // Build aggregated response
        CashFlowResponse aggregatedResponse = CashFlowResponse.builder()
            .requestId(originalRequest.getRequestId())
            .calculationDate(originalRequest.getCalculationDate())
            .totalPnl(totalPnl)
            .totalInterest(totalInterest)
            .totalDividends(totalDividends)
            .totalCashFlows(totalCashFlows)
            .contractResults(allContractResults)
            .status("COMPLETED")
            .build();
        
        log.info("Aggregated results: {} contracts, PnL={}, Interest={}, Dividends={}, Total={}", 
                allContractResults.size(), totalPnl, totalInterest, totalDividends, totalCashFlows);
        
        return aggregatedResponse;
    }
    
    /**
     * Get batch processing statistics
     */
    public BatchProcessingStats getStats() {
        // This would be implemented with actual metrics collection
        return BatchProcessingStats.builder()
            .totalBatchesProcessed(0) // Would be tracked in real implementation
            .averageBatchSize(0)
            .averageProcessingTime(0)
            .build();
    }
    
    /**
     * Batch processing statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class BatchProcessingStats {
        private long totalBatchesProcessed;
        private double averageBatchSize;
        private double averageProcessingTime;
    }
}
