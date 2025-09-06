package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for cash flow calculations.
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

    /**
     * Calculate cash flows asynchronously.
     * 
     * @param request The cash flow calculation request
     * @return CompletableFuture containing the calculation response
     */
    public CompletableFuture<CashFlowResponse> calculateCashFlows(CashFlowRequestContent request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            log.info("Starting cash flow calculation for request: {}", request.getRequestId());

            try {
                // Validate request
                cashFlowValidator.validateRequest(request);

                // Check cache first
                String cacheKey = generateCacheKey(request);
                CashFlowResponse cachedResponse = cacheService.get(cacheKey, CashFlowResponse.class);
                if (cachedResponse != null) {
                    log.info("Cache hit for request: {}", request.getRequestId());
                    return cachedResponse;
                }

                // Calculate cash flows
                List<CashFlowResponse.CashFlow> cashFlows = cashFlowEngine.calculateCashFlows(request);

                // Generate settlement instructions
                List<CashFlowResponse.SettlementInstruction> settlements = 
                    settlementService.generateSettlementInstructions(cashFlows);

                // Build response
                CashFlowResponse response = buildResponse(request, cashFlows, settlements, startTime);

                // Cache the result
                cacheService.put(cacheKey, response);

                // Publish event
                eventPublisher.publishCashFlowCalculatedEvent(request.getRequestId(), 
                    request.getContractId(), cashFlows.size(), response.getTotalAmount(), 
                    response.getCurrency());

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
        });
    }

    /**
     * Calculate cash flows for multiple contracts in batch.
     * 
     * @param requests List of cash flow calculation requests
     * @return List of calculation responses
     */
    public List<CashFlowResponse> calculateCashFlowsBatch(List<CashFlowRequestContent> requests) {
        log.info("Starting batch cash flow calculation for {} requests", requests.size());
        
        List<CompletableFuture<CashFlowResponse>> futures = requests.stream()
            .map(this::calculateCashFlows)
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }

    /**
     * Recalculate cash flows for a specific date range.
     * 
     * @param request The cash flow calculation request
     * @return CompletableFuture containing the calculation response
     */
    public CompletableFuture<CashFlowResponse> recalculateCashFlows(CashFlowRequestContent request) {
        log.info("Starting cash flow recalculation for request: {}", request.getRequestId());
        
        // Clear cache for this request
        String cacheKey = generateCacheKey(request);
        cacheService.evict(cacheKey);
        
        // Perform calculation
        return calculateCashFlows(request);
    }

    /**
     * Get cached calculation result.
     * 
     * @param requestId The request ID
     * @return Cached response or null if not found
     */
    public CashFlowResponse getCachedCalculation(String requestId) {
        log.info("Retrieving cached calculation for request: {}", requestId);
        return cacheService.get("cashflow:" + requestId, CashFlowResponse.class);
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
