package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Main service for cash flow calculations.
 * Uses virtual threads for I/O operations and platform threads for CPU work.
 * Orchestrates the calculation process and manages caching.
 */
@Slf4j
@Service
public class CashFlowCalculationService {

    @Autowired
    private CashFlowEngine cashFlowEngine;

    @Autowired
    private CashFlowValidator cashFlowValidator;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;

    @Autowired
    @Qualifier("cpuThreadExecutor")
    private ExecutorService cpuThreadExecutor;

    /**
     * Calculate cash flows asynchronously.
     * Uses virtual threads for I/O operations and platform threads for CPU work.
     * 
     * @param request The cash flow calculation request
     * @return CompletableFuture containing the calculation response
     */
    public CompletableFuture<CashFlowResponse> calculateCashFlows(CashFlowRequestContent request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            log.info("Starting cash flow calculation for request: {}", request.getRequestId());

            try {
                // Validate request (CPU work - use platform threads)
                CompletableFuture<Void> validationFuture = CompletableFuture.runAsync(() -> {
                    cashFlowValidator.validateRequest(request);
                }, cpuThreadExecutor);
                validationFuture.join();

                // Check cache first (I/O work - use virtual threads)
                String cacheKey = generateCacheKey(request);
                CompletableFuture<CashFlowResponse> cacheFuture = CompletableFuture.supplyAsync(() -> {
                    return cacheService.get(cacheKey, CashFlowResponse.class);
                }, virtualThreadExecutor);
                
                CashFlowResponse cachedResponse = cacheFuture.get();
                if (cachedResponse != null) {
                    log.info("Cache hit for request: {}", request.getRequestId());
                    return cachedResponse;
                }

                // Calculate cash flows (CPU work - use platform threads)
                CompletableFuture<List<CashFlowResponse.CashFlow>> calculationFuture = 
                    CompletableFuture.supplyAsync(() -> {
                        return cashFlowEngine.calculateCashFlows(request);
                    }, cpuThreadExecutor);
                
                List<CashFlowResponse.CashFlow> cashFlows = calculationFuture.get();

                // Generate settlement instructions (I/O work - use virtual threads)
                CompletableFuture<List<CashFlowResponse.SettlementInstruction>> settlementFuture = 
                    CompletableFuture.supplyAsync(() -> {
                        return settlementService.generateSettlementInstructions(cashFlows, request);
                    }, virtualThreadExecutor);
                
                List<CashFlowResponse.SettlementInstruction> settlements = settlementFuture.get();

                // Build response (CPU work - use platform threads)
                CompletableFuture<CashFlowResponse> responseFuture = 
                    CompletableFuture.supplyAsync(() -> {
                        return buildResponse(request, cashFlows, settlements, startTime);
                    }, cpuThreadExecutor);
                
                CashFlowResponse response = responseFuture.get();

                // Cache the result (I/O work - use virtual threads)
                CompletableFuture.runAsync(() -> {
                    cacheService.put(cacheKey, response);
                }, virtualThreadExecutor);

                // Publish event (I/O work - use virtual threads)
                CompletableFuture.runAsync(() -> {
                    eventPublisher.publishCashFlowCalculatedEvent(request.getRequestId(), 
                        request.getContractId(), cashFlows.size(), response.getTotalAmount(), 
                        response.getCurrency());
                }, virtualThreadExecutor);

                log.info("Cash flow calculation completed for request: {} in {}ms", 
                    request.getRequestId(), response.getCalculationDuration());

                return response;

            } catch (Exception e) {
                log.error("Error calculating cash flows for request: {}", request.getRequestId(), e);
                
                // Build error response
                CashFlowResponse errorResponse = CashFlowResponse.builder()
                    .requestId(request.getRequestId())
                    .contractId(request.getContractId())
                    .calculationId(UUID.randomUUID().toString())
                    .calculationType("FAILED")
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .calculationDuration(System.currentTimeMillis() - startTime)
                    .build();

                return errorResponse;
            }
        }, virtualThreadExecutor); // Use virtual threads for the main orchestration
    }

    /**
     * Calculate cash flows for multiple contracts in batch.
     * Uses virtual threads for parallel processing.
     * 
     * @param requests List of cash flow calculation requests
     * @return List of calculation responses
     */
    public List<CashFlowResponse> calculateCashFlowsBatch(List<CashFlowRequestContent> requests) {
        log.info("Starting batch cash flow calculation for {} requests", requests.size());
        
        // Use virtual threads for parallel processing
        List<CompletableFuture<CashFlowResponse>> futures = requests.stream()
            .map(this::calculateCashFlows)
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }

    /**
     * Recalculate cash flows for a specific date range.
     * Uses virtual threads for I/O operations.
     * 
     * @param request The cash flow calculation request
     * @return CompletableFuture containing the calculation response
     */
    public CompletableFuture<CashFlowResponse> recalculateCashFlows(CashFlowRequestContent request) {
        log.info("Starting cash flow recalculation for request: {}", request.getRequestId());
        
        // Clear cache for this request (I/O work - use virtual threads)
        String cacheKey = generateCacheKey(request);
        CompletableFuture.runAsync(() -> {
            cacheService.evict(cacheKey);
        }, virtualThreadExecutor);
        
        // Perform calculation
        return calculateCashFlows(request);
    }

    /**
     * Get cached calculation result.
     * Uses virtual threads for I/O operations.
     * 
     * @param requestId The request ID
     * @return Cached response or null if not found
     */
    public CashFlowResponse getCachedCalculation(String requestId) {
        log.info("Retrieving cached calculation for request: {}", requestId);
        return CompletableFuture.supplyAsync(() -> {
            return cacheService.get("cashflow:" + requestId, CashFlowResponse.class);
        }, virtualThreadExecutor).join();
    }

    /**
     * Generate cache key for the request.
     */
    private String generateCacheKey(CashFlowRequestContent request) {
        return String.format("cashflow:%s:%s:%s:%s",
            request.getContractId(),
            request.getDateRange().getFromDate(),
            request.getDateRange().getToDate(),
            request.getCalculationType() != null ? request.getCalculationType() : "auto"
        );
    }

    /**
     * Determine the calculation type based on request parameters.
     */
    private String determineCalculationType(CashFlowRequestContent request) {
        if (request.getCalculationType() != null) {
            return request.getCalculationType();
        } else {
            // Auto-determine based on date range and data
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fromDate = request.getDateRange().getFromDate().atStartOfDay();
            
            if (fromDate.isBefore(now.minusDays(1))) {
                return "HISTORICAL_RECALCULATION";
            } else if (fromDate.isBefore(now)) {
                return "INCREMENTAL_UPDATE";
            } else {
                return "REAL_TIME_PROCESSING";
            }
        }
    }

    /**
     * Build the response object from calculation results.
     */
    private CashFlowResponse buildResponse(
            CashFlowRequestContent request,
            List<CashFlowResponse.CashFlow> cashFlows,
            List<CashFlowResponse.SettlementInstruction> settlements,
            long startTime) {
        
        long duration = System.currentTimeMillis() - startTime;
        double totalAmount = cashFlows.stream().mapToDouble(cf -> cf.getAmount()).sum();
        String currency = cashFlows.isEmpty() ? "USD" : cashFlows.get(0).getCurrency();
        String calculationType = determineCalculationType(request);
        
        return CashFlowResponse.builder()
            .requestId(request.getRequestId())
            .contractId(request.getContractId())
            .calculationId(UUID.randomUUID().toString())
            .calculationType(calculationType)
            .cashFlows(cashFlows)
            .settlementInstructions(settlements)
            .totalAmount(totalAmount)
            .currency(currency)
            .calculationDuration(duration)
            .status("SUCCESS")
            .build();
    }
}
