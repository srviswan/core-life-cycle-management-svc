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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
            long processingTime = System.currentTimeMillis() - startTime;
            CashFlowResponse response = buildResponse(request, contractResults, processingTime);
            
            log.info("Calculation completed for {} contracts in {}ms", 
                request.getContracts().size(), processingTime);
            
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
            long processingTime = System.currentTimeMillis() - startTime;
            CashFlowResponse response = buildResponse(request, contractResults, processingTime);
            
            log.info("Real-time calculation completed in {}ms", processingTime);
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
            long processingTime = System.currentTimeMillis() - startTime;
            CashFlowResponse response = buildResponse(request, contractResults, processingTime);
            
            log.info("Historical calculation completed for {} contracts in {}ms", 
                request.getContracts().size(), processingTime);
            
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
        log.info("Processing {} contracts in parallel", contracts.size());
        return contracts.parallelStream()
            .map(contract -> {
                try {
                    log.info("Processing contract: {}", contract.getContractId());
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
        log.info("Processing {} contracts sequentially", contracts.size());
        return contracts.stream()
            .map(contract -> {
                try {
                    log.info("Processing contract: {}", contract.getContractId());
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
        log.info("Calculating contract: {}", contract.getContractId());
        
        try {
            // Calculate P&L using lot-based calculation if lots are available
            double pnl;
            if (request.getLots() != null && !request.getLots().isEmpty()) {
                // Use lot-based P&L calculation
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                pnl = pnLCalculator.calculatePnL(contract, marketData, calculationDate, request.getLots());
            } else {
                // Fall back to legacy calculation
                pnl = pnLCalculator.calculatePnL(contract, marketData);
            }
            
            // Calculate interest using lot-based notional if lots are available
            double interest;
            if (request.getLots() != null && !request.getLots().isEmpty()) {
                // Use lot-based notional calculation
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                interest = interestCalculator.calculateInterest(contract, marketData, calculationDate, request.getLots());
            } else {
                // Fall back to legacy calculation
                interest = interestCalculator.calculateInterest(contract, marketData);
            }
            
            // Calculate dividends using lot-based calculation if lots are available
            double dividends;
            if (request.getLots() != null && !request.getLots().isEmpty()) {
                // Use lot-based dividend calculation
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                dividends = dividendCalculator.calculateDividends(contract, marketData, calculationDate, request.getLots());
            } else {
                // Fall back to legacy calculation
                dividends = dividendCalculator.calculateDividends(contract, marketData);
            }
            
            // Create lot-level cash flows if lots are available
            List<CashFlowResponse.CashFlow> lotCashFlows = new ArrayList<>();
            log.info("Checking lots for contract {}: lots={}, size={}", 
                contract.getContractId(), request.getLots(), 
                request.getLots() != null ? request.getLots().size() : 0);
            
            if (request.getLots() != null && !request.getLots().isEmpty()) {
                log.info("Creating lot-level cash flows for contract: {}", contract.getContractId());
                lotCashFlows = createLotLevelCashFlows(contract, request, marketData, pnl, interest, dividends);
                log.info("Created {} lot-level cash flows for contract: {}", lotCashFlows.size(), contract.getContractId());
            } else {
                log.info("No lots available for contract: {}", contract.getContractId());
            }
            
            // Build contract result
            CashFlowResponse.ContractResult result = CashFlowResponse.ContractResult.builder()
                .contractId(contract.getContractId())
                .underlying(contract.getUnderlying())
                .totalPnl(java.math.BigDecimal.valueOf(pnl))
                .totalInterest(java.math.BigDecimal.valueOf(interest))
                .totalDividends(java.math.BigDecimal.valueOf(dividends))
                .totalCashFlows(java.math.BigDecimal.valueOf(pnl + interest + dividends))
                .cashFlows(lotCashFlows)
                .status("SUCCESS")
                .build();
            
            log.info("Contract {} calculated in {}ms", contract.getContractId(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Failed to calculate contract: {}", contract.getContractId(), e);
            throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
        }
    }
    
    /**
     * Build response from contract results
     */
    private CashFlowResponse buildResponse(CashFlowRequest request, List<CashFlowResponse.ContractResult> contractResults, long processingTimeMs) {
        return CashFlowResponse.builder()
            .requestId(request.getRequestId())
            .calculationDate(LocalDate.now())
            .dateRange(CashFlowResponse.DateRange.builder()
                .fromDate(request.getDateRange().getFromDate())
                .toDate(request.getDateRange().getToDate())
                .build())
            .calculationType(convertCalculationType(request.getCalculationType()))
            .summary(buildSummary(contractResults, processingTimeMs))
            .contractResults(contractResults)
            .metadata(buildMetadata(request, contractResults, processingTimeMs))
            .status("SUCCESS")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Build calculation summary
     */
    private CashFlowResponse.CalculationSummary buildSummary(List<CashFlowResponse.ContractResult> contractResults, long processingTimeMs) {
        return CashFlowResponse.CalculationSummary.builder()
            .totalContracts(contractResults.size())
            .totalCashFlows(contractResults.stream().mapToInt(r -> r.getCashFlows() != null ? r.getCashFlows().size() : 0).sum())
            .totalAmount(contractResults.stream()
                .map(CashFlowResponse.ContractResult::getTotalCashFlows)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
            .currency("USD")
            .processingTimeMs(processingTimeMs)
            .build();
    }
    
    /**
     * Build calculation metadata
     */
    private CashFlowResponse.CalculationMetadata buildMetadata(CashFlowRequest request, List<CashFlowResponse.ContractResult> contractResults, long processingTimeMs) {
        return CashFlowResponse.CalculationMetadata.builder()
            .calculationVersion("1.0")
            .calculationEngine("ConventionalCalculationEngine")
            .processingTimeMs(processingTimeMs)
            .memoryUsageMB(Runtime.getRuntime().totalMemory() / 1024 / 1024)
            .dataSource("HYBRID")
            .contractsProcessed(contractResults.size())
            .errorsEncountered(0)
            .build();
    }
    
    /**
     * Convert calculation type from request to response
     */
    private CashFlowResponse.CalculationType convertCalculationType(CashFlowRequest.CalculationType requestType) {
        return switch (requestType) {
            case REAL_TIME_PROCESSING -> CashFlowResponse.CalculationType.REAL_TIME_PROCESSING;
            case HISTORICAL_RECALCULATION -> CashFlowResponse.CalculationType.HISTORICAL_RECALCULATION;
            case BATCH_PROCESSING -> CashFlowResponse.CalculationType.BATCH_PROCESSING;
        };
    }
    
    /**
     * Create lot-level cash flow records for detailed tracking
     */
    private List<CashFlowResponse.CashFlow> createLotLevelCashFlows(CashFlowRequest.Contract contract, 
                                                                   CashFlowRequest request, 
                                                                   MarketData marketData,
                                                                   double totalPnl, 
                                                                   double totalInterest, 
                                                                   double totalDividends) {
        List<CashFlowResponse.CashFlow> lotCashFlows = new ArrayList<>();
        
        log.info("Creating lot-level cash flows for contract: {}, total lots: {}", 
            contract.getContractId(), request.getLots().size());
        
        // Get lots for this contract
        List<CashFlowRequest.Lot> contractLots = request.getLots().stream()
            .filter(lot -> contract.getContractId().equals(lot.getContractId()))
            .collect(Collectors.toList());
        
        log.info("Found {} lots for contract: {}", contractLots.size(), contract.getContractId());
        
        if (contractLots.isEmpty()) {
            log.warn("No lots found for contract: {}", contract.getContractId());
            return lotCashFlows;
        }
        
        // Calculate per-lot amounts (proportional to quantity)
        double totalQuantity = contractLots.stream()
            .mapToDouble(CashFlowRequest.Lot::getQuantity)
            .sum();
        
        for (CashFlowRequest.Lot lot : contractLots) {
            double lotRatio = lot.getQuantity() / totalQuantity;
            
            // Create individual cash flow record for this lot
            CashFlowResponse.CashFlow lotCashFlow = CashFlowResponse.CashFlow.builder()
                .cashFlowId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .contractId(contract.getContractId())
                .positionId(lot.getPositionId()) // Use lot's position ID if available
                .lotId(lot.getLotId())
                .calculationDate(request.getCalculationDate())
                .cashFlowType("EQUITY_SWAP")
                .equityLegAmount(java.math.BigDecimal.valueOf(totalPnl * lotRatio))
                .interestLegAmount(java.math.BigDecimal.valueOf(totalInterest * lotRatio))
                .totalAmount(java.math.BigDecimal.valueOf((totalPnl + totalInterest + totalDividends) * lotRatio))
                .currency(contract.getCurrency())
                .state("REALIZED_UNSETTLED")
                .equityUnrealizedPnl(java.math.BigDecimal.valueOf(totalPnl * lotRatio))
                .equityRealizedPnl(java.math.BigDecimal.ZERO)
                .equityTotalPnl(java.math.BigDecimal.valueOf(totalPnl * lotRatio))
                .equityDividendAmount(java.math.BigDecimal.valueOf(totalDividends * lotRatio))
                .equityWithholdingTax(java.math.BigDecimal.ZERO)
                .equityNetDividend(java.math.BigDecimal.valueOf(totalDividends * lotRatio))
                .interestAccruedAmount(java.math.BigDecimal.valueOf(totalInterest * lotRatio))
                .interestRate(java.math.BigDecimal.valueOf(getInterestRate(contract, marketData)))
                .interestNotionalAmount(java.math.BigDecimal.valueOf(contract.getNotionalAmount() * lotRatio))
                .build();
            
            lotCashFlows.add(lotCashFlow);
        }
        
        log.info("Created {} lot-level cash flows for contract: {}", lotCashFlows.size(), contract.getContractId());
        return lotCashFlows;
    }
    
    /**
     * Get interest rate from market data
     */
    private double getInterestRate(CashFlowRequest.Contract contract, MarketData marketData) {
        if (marketData.getRate() != null && contract.getIndex().equals(marketData.getRate().getIndex())) {
            return marketData.getRate().getBaseRate();
        }
        return 0.0;
    }
}
