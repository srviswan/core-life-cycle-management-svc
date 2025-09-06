package com.financial.cashflow.service;

import com.financial.cashflow.calculator.DividendCalculator;
import com.financial.cashflow.calculator.InterestCalculator;
import com.financial.cashflow.calculator.PnLCalculator;
import com.financial.cashflow.exception.CalculationException;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Calculation Engine for Cash Flow Management
 * Uses platform threads for CPU-intensive calculations with parallel processing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CalculationEngine {
    
    private final PnLCalculator pnLCalculator;
    private final InterestCalculator interestCalculator;
    private final DividendCalculator dividendCalculator;
    
    // Platform threads for CPU-intensive calculations
    private final ExecutorService cpuThreadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    /**
     * Calculate cash flows with parallel processing
     */
    public CashFlowResponse calculate(CashFlowRequest request, MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.info("Starting calculation for {} contracts", request.getContracts().size());
        
        try {
            // Process contracts in parallel using platform threads for CPU work
            List<CashFlowResponse.ContractResult> contractResults = processContractsParallel(request.getContracts(), request, marketData);
            
            // Build response
            CashFlowResponse response = buildResponse(request, contractResults);
            
            log.info("Calculation completed for {} contracts in {}ms", 
                request.getContracts().size(), System.currentTimeMillis() - startTime);
            
            return response;
        } catch (Exception e) {
            log.error("Calculation failed", e);
            throw new CalculationException("Calculation failed", e);
        }
    }
    
    /**
     * Calculate cash flows in real-time (sequential for speed)
     */
    public CashFlowResponse calculateRealTime(CashFlowRequest request, MarketData marketData) {
        // For real-time, use direct execution for speed
        long startTime = System.currentTimeMillis();
        log.info("Starting real-time calculation");
        
        try {
            List<CashFlowResponse.ContractResult> contractResults = processContractsSequential(request.getContracts(), request, marketData);
            CashFlowResponse response = buildResponse(request, contractResults);
            
            log.info("Real-time calculation completed in {}ms", System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.error("Real-time calculation failed", e);
            throw new CalculationException("Real-time calculation failed", e);
        }
    }
    
    /**
     * Calculate historical cash flows with parallel processing
     */
    public CashFlowResponse calculateHistorical(CashFlowRequest request, MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.info("Starting historical calculation for {} contracts", request.getContracts().size());
        
        try {
            // For historical calculations, use parallel processing
            List<CashFlowResponse.ContractResult> contractResults = processContractsParallel(request.getContracts(), request, marketData);
            CashFlowResponse response = buildResponse(request, contractResults);
            
            log.info("Historical calculation completed for {} contracts in {}ms", 
                request.getContracts().size(), System.currentTimeMillis() - startTime);
            
            return response;
        } catch (Exception e) {
            log.error("Historical calculation failed", e);
            throw new CalculationException("Historical calculation failed", e);
        }
    }
    
    /**
     * Process contracts in parallel using platform threads
     */
    private List<CashFlowResponse.ContractResult> processContractsParallel(List<CashFlowRequest.Contract> contracts, 
                                                                          CashFlowRequest request, 
                                                                          MarketData marketData) {
        return contracts.parallelStream()
            .map(contract -> {
                try {
                    return calculateContract(contract, request, marketData);
                } catch (Exception e) {
                    log.error("Failed to calculate contract: {}", contract.getContractId(), e);
                    throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Process contracts sequentially for real-time calculations
     */
    private List<CashFlowResponse.ContractResult> processContractsSequential(List<CashFlowRequest.Contract> contracts, 
                                                                             CashFlowRequest request, 
                                                                             MarketData marketData) {
        return contracts.stream()
            .map(contract -> {
                try {
                    return calculateContract(contract, request, marketData);
                } catch (Exception e) {
                    log.error("Failed to calculate contract: {}", contract.getContractId(), e);
                    throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate individual contract
     */
    private CashFlowResponse.ContractResult calculateContract(CashFlowRequest.Contract contract, 
                                                             CashFlowRequest request, 
                                                             MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.debug("Calculating contract: {}", contract.getContractId());
        
        try {
            // Calculate P&L
            double pnl = pnLCalculator.calculatePnL(contract, marketData);
            
            // Calculate interest
            double interest = interestCalculator.calculateInterest(contract, marketData);
            
            // Calculate dividends
            double dividends = dividendCalculator.calculateDividends(contract, marketData);
            
            // Build contract result
            CashFlowResponse.ContractResult result = CashFlowResponse.ContractResult.builder()
                .contractId(contract.getContractId())
                .underlying(contract.getUnderlying())
                .totalPnl(java.math.BigDecimal.valueOf(pnl))
                .totalInterest(java.math.BigDecimal.valueOf(interest))
                .totalDividends(java.math.BigDecimal.valueOf(dividends))
                .totalCashFlows(java.math.BigDecimal.valueOf(pnl + interest + dividends))
                .status("SUCCESS")
                .build();
            
            log.debug("Contract {} calculated in {}ms", contract.getContractId(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Failed to calculate contract: {}", contract.getContractId(), e);
            throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
        }
    }
    
    /**
     * Build response from contract results
     */
    private CashFlowResponse buildResponse(CashFlowRequest request, List<CashFlowResponse.ContractResult> contractResults) {
        return CashFlowResponse.builder()
            .requestId(request.getRequestId())
            .calculationDate(LocalDate.now())
            .dateRange(CashFlowResponse.DateRange.builder()
                .fromDate(request.getDateRange().getFromDate())
                .toDate(request.getDateRange().getToDate())
                .build())
            .calculationType(convertCalculationType(request.getCalculationType()))
            .summary(buildSummary(contractResults))
            .contractResults(contractResults)
            .metadata(buildMetadata(request, contractResults))
            .status("SUCCESS")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Build calculation summary
     */
    private CashFlowResponse.CalculationSummary buildSummary(List<CashFlowResponse.ContractResult> contractResults) {
        return CashFlowResponse.CalculationSummary.builder()
            .totalContracts(contractResults.size())
            .totalCashFlows(contractResults.stream().mapToInt(r -> r.getCashFlows() != null ? r.getCashFlows().size() : 0).sum())
            .totalAmount(contractResults.stream()
                .map(CashFlowResponse.ContractResult::getTotalCashFlows)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
            .currency("USD")
            .processingTimeMs(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Build calculation metadata
     */
    private CashFlowResponse.CalculationMetadata buildMetadata(CashFlowRequest request, List<CashFlowResponse.ContractResult> contractResults) {
        return CashFlowResponse.CalculationMetadata.builder()
            .calculationVersion("1.0")
            .calculationEngine("ConventionalCalculationEngine")
            .processingTimeMs(System.currentTimeMillis())
            .memoryUsageMB(Runtime.getRuntime().totalMemory() / 1024 / 1024)
            .dataSource("HYBRID")
            .contractsProcessed(contractResults.size())
            .errorsEncountered(0)
            .build();
    }
}
