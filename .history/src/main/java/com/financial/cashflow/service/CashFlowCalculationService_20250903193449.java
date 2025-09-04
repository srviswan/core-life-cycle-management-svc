package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.exception.CashFlowCalculationException;
import com.financial.cashflow.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for cash flow calculations.
 * Handles the core business logic for calculating cash flows from synthetic swaps.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class CashFlowCalculationService {

    private final CashFlowValidator validator;
    private final CashFlowEngine engine;
    private final SettlementService settlementService;
    private final EventPublisher eventPublisher;
    private final CacheService cacheService;

    /**
     * Calculate cash flows for a given request.
     * This is the main entry point for cash flow calculations.
     */
    @Transactional
    public CompletableFuture<CashFlowResponse> calculateCashFlows(@Valid CashFlowRequestContent request) {
        log.info("Starting cash flow calculation for request: {}", request.getRequestId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Validate request
                validator.validateRequest(request);
                
                // Step 2: Check cache
                String cacheKey = generateCacheKey(request);
                CashFlowResponse cachedResponse = cacheService.get(cacheKey, CashFlowResponse.class);
                if (cachedResponse != null) {
                    log.info("Cache hit for request: {}", request.getRequestId());
                    return CashFlowResponse.builder()
                        .requestId(cachedResponse.getRequestId())
                        .contractId(cachedResponse.getContractId())
                        .calculationId(cachedResponse.getCalculationId())
                        .calculationType(cachedResponse.getCalculationType())
                        .calculationDate(cachedResponse.getCalculationDate())
                        .dateRange(cachedResponse.getDateRange())
                        .cashFlows(cachedResponse.getCashFlows())
                        .settlementInstructions(cachedResponse.getSettlementInstructions())
                        .summary(cachedResponse.getSummary())
                        .status(cachedResponse.getStatus())
                        .errors(cachedResponse.getErrors())
                        .build();
                }
                
                // Step 3: Determine calculation type
                CashFlowResponse.CalculationType calculationType = determineCalculationType(request);
                
                // Step 4: Perform calculation
                long startTime = System.currentTimeMillis();
                List<CashFlowResponse.CashFlow> cashFlows = engine.calculateCashFlows(request);
                
                // Step 5: Generate settlement instructions
                List<CashFlowResponse.SettlementInstruction> settlements = 
                    settlementService.generateSettlementInstructions(cashFlows, request);
                
                // Step 6: Build response
                CashFlowResponse response = buildResponse(request, calculationType, cashFlows, settlements, startTime);
                
                // Step 7: Cache result
                cacheService.put(cacheKey, response);
                
                // Step 8: Publish events
                eventPublisher.publishCashFlowCalculated(response);
                
                log.info("Cash flow calculation completed for request: {} in {}ms", 
                    request.getRequestId(), System.currentTimeMillis() - startTime);
                
                return response;
                
            } catch (Exception e) {
                log.error("Error calculating cash flows for request: {}", request.getRequestId(), e);
                throw new CashFlowCalculationException("Failed to calculate cash flows", e);
            }
        });
    }

    /**
     * Calculate cash flows in batch for multiple requests.
     */
    @Transactional
    public CompletableFuture<List<CashFlowResponse>> calculateCashFlowsBatch(
            List<@Valid CashFlowRequestContent> requests) {
        log.info("Starting batch cash flow calculation for {} requests", requests.size());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requests.parallelStream()
                    .map(request -> calculateCashFlows(request).join())
                    .toList();
            } catch (Exception e) {
                log.error("Error in batch cash flow calculation", e);
                throw new CashFlowCalculationException("Failed to calculate cash flows in batch", e);
            }
        });
    }

    /**
     * Recalculate cash flows for a specific date range.
     */
    @Transactional
    public CompletableFuture<CashFlowResponse> recalculateCashFlows(
            String contractId, 
            CashFlowRequestContent.DateRange dateRange) {
        log.info("Starting cash flow recalculation for contract: {} date range: {}", contractId, dateRange);
        
        // Build request for recalculation
        CashFlowRequestContent request = CashFlowRequestContent.builder()
            .requestId("RECALC_" + UUID.randomUUID().toString())
            .contractId(contractId)
            .calculationType("HISTORICAL_RECALCULATION")
            .dateRange(dateRange)
            .build();
        
        return calculateCashFlows(request);
    }

    /**
     * Get cached cash flow calculation result.
     */
    @Cacheable(value = "cashFlowCalculations", key = "#requestId")
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
    private CashFlowResponse.CalculationType determineCalculationType(CashFlowRequestContent request) {
        String determinedType;
        String reason;
        Double confidence = 1.0;

        if (request.getCalculationType() != null) {
            determinedType = request.getCalculationType();
            reason = "Explicitly specified by caller";
        } else {
            // Auto-determine based on date range and data
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fromDate = request.getDateRange().getFromDate().atStartOfDay();
            
            if (fromDate.isBefore(now.minusDays(1))) {
                determinedType = "HISTORICAL_RECALCULATION";
                reason = "Date range is in the past";
            } else if (fromDate.isBefore(now)) {
                determinedType = "INCREMENTAL_UPDATE";
                reason = "Date range spans current time";
            } else {
                determinedType = "REAL_TIME_PROCESSING";
                reason = "Date range is in the future";
            }
        }

        return CashFlowResponse.CalculationType.builder()
            .determinedType(determinedType)
            .reason(reason)
            .confidence(confidence)
            .build();
    }

    /**
     * Build the response object from calculation results.
     */
    private CashFlowResponse buildResponse(
            CashFlowRequestContent request,
            CashFlowResponse.CalculationType calculationType,
            List<CashFlowResponse.CashFlow> cashFlows,
            List<CashFlowResponse.SettlementInstruction> settlements,
            long startTime) {
        
        long duration = System.currentTimeMillis() - startTime;
        
        CashFlowResponse.CalculationSummary summary = CashFlowResponse.CalculationSummary.builder()
            .totalCashFlows(cashFlows.size())
            .totalAmount(cashFlows.stream().mapToDouble(cf -> cf.getAmount()).sum())
            .currency(cashFlows.isEmpty() ? "USD" : cashFlows.get(0).getCurrency())
            .interestAmount(cashFlows.stream()
                .filter(cf -> "INTEREST".equals(cf.getCashFlowType()))
                .mapToDouble(cf -> cf.getAmount()).sum())
            .dividendAmount(cashFlows.stream()
                .filter(cf -> "DIVIDEND".equals(cf.getCashFlowType()))
                .mapToDouble(cf -> cf.getAmount()).sum())
            .principalAmount(cashFlows.stream()
                .filter(cf -> "PRINCIPAL".equals(cf.getCashFlowType()))
                .mapToDouble(cf -> cf.getAmount()).sum())
            .pnlAmount(cashFlows.stream()
                .filter(cf -> "PNL".equals(cf.getCashFlowType()))
                .mapToDouble(cf -> cf.getAmount()).sum())
            .settlementInstructions(settlements.size())
            .calculationDuration(duration)
            .cacheHit(false)
            .build();

        return CashFlowResponse.builder()
            .requestId(request.getRequestId())
            .contractId(request.getContractId())
            .calculationId(UUID.randomUUID().toString())
            .calculationType(calculationType)
            .calculationDate(LocalDateTime.now())
            .dateRange(request.getDateRange())
            .cashFlows(cashFlows)
            .settlementInstructions(settlements)
            .summary(summary)
            .status("SUCCESS")
            .build();
    }
}
