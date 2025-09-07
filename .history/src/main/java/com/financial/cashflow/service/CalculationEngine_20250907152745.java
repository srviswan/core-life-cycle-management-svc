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
import java.util.concurrent.CompletableFuture;
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
    
    {
        log.info("CalculationEngine instance created");
    }
    
    /**
     * Calculate cash flows with parallel processing
     */
    public CashFlowResponse calculate(CashFlowRequest request, MarketData marketData) {
        System.out.println("*** CalculationEngine.calculate() called with request: " + request.getRequestId() + " ***");
        long startTime = System.currentTimeMillis();
        
        // Log contract information based on structure
        if (request.getContractPositions() != null && !request.getContractPositions().isEmpty()) {
            log.info("Starting calculation for {} contract positions", request.getContractPositions().size());
            log.info("Contract positions list: {}", request.getContractPositions());
        } else if (request.getContracts() != null && !request.getContracts().isEmpty()) {
            log.info("Starting calculation for {} contracts", request.getContracts().size());
            log.info("Contracts list: {}", request.getContracts());
        } else {
            log.info("Starting calculation with no contracts or contract positions");
        }
        
        try {
            List<CashFlowResponse.ContractResult> contractResults;
            
            // Check if we have the new hierarchical structure
            if (request.getContractPositions() != null && !request.getContractPositions().isEmpty()) {
                log.info("Processing {} contract positions with hierarchical structure", request.getContractPositions().size());
                contractResults = processContractPositionsParallel(request.getContractPositions(), request, marketData);
            } else {
                // Fallback to legacy contract processing
                log.info("About to call processContractsParallel with {} contracts", request.getContracts().size());
                contractResults = processContractsParallel(request.getContracts(), request, marketData);
            }
            log.info("Contract processing returned {} results", contractResults.size());
            
            // Build response
            long processingTime = System.currentTimeMillis() - startTime;
            CashFlowResponse response = buildResponse(request, contractResults, processingTime);
            
            // Log completion based on structure
            if (request.getContractPositions() != null && !request.getContractPositions().isEmpty()) {
                log.info("Calculation completed for {} contract positions in {}ms", 
                    request.getContractPositions().size(), processingTime);
            } else if (request.getContracts() != null && !request.getContracts().isEmpty()) {
                log.info("Calculation completed for {} contracts in {}ms", 
                    request.getContracts().size(), processingTime);
            } else {
                log.info("Calculation completed in {}ms", processingTime);
            }
            
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
     * Process contract positions in parallel with hierarchical structure
     * Creates hierarchical results: Contract -> Position -> Lots
     * Ensures parallel processing at both position and lot levels
     */
    private List<CashFlowResponse.ContractResult> processContractPositionsParallel(List<CashFlowRequest.ContractPosition> contractPositions, 
                                                                                   CashFlowRequest request, 
                                                                                   MarketData marketData) {
        log.info("Processing {} contract positions in parallel with hierarchical structure", contractPositions.size());
        
        List<CashFlowResponse.ContractResult> contractResults = new ArrayList<>();
        
        for (CashFlowRequest.ContractPosition contractPosition : contractPositions) {
            log.info("Processing contract: {} with {} positions", 
                contractPosition.getContractId(), contractPosition.getPositions().size());
            
            // Process each position within the contract in parallel using CompletableFuture
            List<CompletableFuture<CashFlowResponse.PositionResult>> positionFutures = contractPosition.getPositions()
                .stream()
                .map(position -> CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info("Processing position: {} in contract: {} on thread: {}", 
                            position.getPositionId(), contractPosition.getContractId(), Thread.currentThread().getName());
                        return calculatePositionResult(contractPosition, position, request, marketData);
                    } catch (Exception e) {
                        log.error("Failed to calculate position: {} in contract: {}", 
                            position.getPositionId(), contractPosition.getContractId(), e);
                        throw new CalculationException("Failed to calculate position: " + position.getPositionId(), e);
                    }
                }))
                .collect(Collectors.toList());
            
            // Wait for all position calculations to complete
            log.info("Waiting for {} position calculations to complete for contract: {}", 
                positionFutures.size(), contractPosition.getContractId());
            
            List<CashFlowResponse.PositionResult> positionResults = positionFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            log.info("All {} position calculations completed for contract: {}", 
                positionResults.size(), contractPosition.getContractId());
            
            // Create hierarchical contract result with all positions
            CashFlowResponse.ContractResult contractResult = buildHierarchicalContractResult(contractPosition, positionResults);
            contractResults.add(contractResult);
        }
        
        return contractResults;
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
     * Calculate individual contract position (partition by contract + position)
     * This creates a separate calculation result for each contract + position combination
     */
    private CashFlowResponse.ContractResult calculateContractPosition(CashFlowRequest.ContractPosition contractPosition, 
                                                                      CashFlowRequest.Position position, 
                                                                      CashFlowRequest request, 
                                                                      MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.info("Calculating contract position: {} + {}", contractPosition.getContractId(), position.getPositionId());
        
        try {
            // Calculate P&L using position-specific lots
            double pnl = 0.0;
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                pnl = pnLCalculator.calculatePnLForPosition(contractPosition, position, marketData, calculationDate);
            }
            
            // Calculate interest using position-specific lots
            double interest = 0.0;
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                interest = interestCalculator.calculateInterestForPosition(contractPosition, position, marketData, calculationDate);
            }
            
            // Calculate dividends using position-specific lots
            double dividends = 0.0;
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                dividends = dividendCalculator.calculateDividendsForPosition(contractPosition, position, marketData, calculationDate);
            }
            
            // Create lot-level cash flows for this position
            List<CashFlowResponse.CashFlow> lotCashFlows = new ArrayList<>();
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                log.info("Creating lot-level cash flows for position: {} in contract: {}", 
                    position.getPositionId(), contractPosition.getContractId());
                lotCashFlows = createPositionLotLevelCashFlows(contractPosition, position, request, marketData, pnl, interest, dividends);
                log.info("Created {} lot-level cash flows for position: {}", lotCashFlows.size(), position.getPositionId());
            }
            
            // Build contract result for this position (partition)
            CashFlowResponse.ContractResult result = CashFlowResponse.ContractResult.builder()
                .contractId(contractPosition.getContractId())
                .underlying(contractPosition.getUnderlying())
                .totalPnl(java.math.BigDecimal.valueOf(pnl))
                .totalInterest(java.math.BigDecimal.valueOf(interest))
                .totalDividends(java.math.BigDecimal.valueOf(dividends))
                .totalCashFlows(java.math.BigDecimal.valueOf(pnl + interest + dividends))
                .cashFlows(lotCashFlows)
                .status("SUCCESS")
                .build();
            
            log.info("Contract position {} + {} calculated in {}ms", 
                contractPosition.getContractId(), position.getPositionId(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Failed to calculate contract position: {} + {}", 
                contractPosition.getContractId(), position.getPositionId(), e);
            throw new CalculationException("Failed to calculate contract position: " + 
                contractPosition.getContractId() + " + " + position.getPositionId(), e);
        }
    }
    
    /**
     * Calculate position result for hierarchical structure
     * Returns PositionResult instead of ContractResult
     */
    private CashFlowResponse.PositionResult calculatePositionResult(CashFlowRequest.ContractPosition contractPosition, 
                                                                   CashFlowRequest.Position position, 
                                                                   CashFlowRequest request, 
                                                                   MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.info("Calculating position result: {} in contract: {}", position.getPositionId(), contractPosition.getContractId());
        
        try {
            // Calculate P&L using position-specific lots
            double pnl = 0.0;
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                pnl = pnLCalculator.calculatePnLForPosition(contractPosition, position, marketData, calculationDate);
            }
            
            // Calculate interest using position-specific lots
            double interest = 0.0;
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                interest = interestCalculator.calculateInterestForPosition(contractPosition, position, marketData, calculationDate);
            }
            
            // Calculate dividends using position-specific lots
            double dividends = 0.0;
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                java.time.LocalDate calculationDate = request.getDateRange() != null ? 
                    request.getDateRange().getFromDate() : java.time.LocalDate.now();
                dividends = dividendCalculator.calculateDividendsForPosition(contractPosition, position, marketData, calculationDate);
            }
            
            // Create lot-level cash flows for this position
            List<CashFlowResponse.CashFlow> lotCashFlows = new ArrayList<>();
            if (position.getLots() != null && !position.getLots().isEmpty()) {
                log.info("Creating lot-level cash flows for position: {} in contract: {}", 
                    position.getPositionId(), contractPosition.getContractId());
                lotCashFlows = createPositionLotLevelCashFlows(contractPosition, position, request, marketData, pnl, interest, dividends);
                log.info("Created {} lot-level cash flows for position: {}", lotCashFlows.size(), position.getPositionId());
            }
            
            // Build position result
            CashFlowResponse.PositionResult result = CashFlowResponse.PositionResult.builder()
                .positionId(position.getPositionId())
                .product(position.getProduct())
                .underlying(position.getUnderlying())
                .type(position.getType() != null ? position.getType().name() : null)
                .notionalAmount(java.math.BigDecimal.valueOf(position.getNotionalAmount() != null ? position.getNotionalAmount() : 0.0))
                .currency(position.getCurrency())
                .totalPnl(java.math.BigDecimal.valueOf(pnl))
                .totalInterest(java.math.BigDecimal.valueOf(interest))
                .totalDividends(java.math.BigDecimal.valueOf(dividends))
                .totalCashFlows(java.math.BigDecimal.valueOf(pnl + interest + dividends))
                .lots(lotCashFlows)
                .status("SUCCESS")
                .build();
            
            log.info("Position result {} calculated in {}ms", 
                position.getPositionId(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Failed to calculate position result: {}", position.getPositionId(), e);
            throw new CalculationException("Failed to calculate position result: " + position.getPositionId(), e);
        }
    }
    
    /**
     * Build hierarchical contract result with all positions
     */
    private CashFlowResponse.ContractResult buildHierarchicalContractResult(CashFlowRequest.ContractPosition contractPosition, 
                                                                           List<CashFlowResponse.PositionResult> positionResults) {
        log.info("Building hierarchical contract result for: {} with {} positions", 
            contractPosition.getContractId(), positionResults.size());
        
        // Calculate contract totals from all positions
        double totalPnl = positionResults.stream()
            .mapToDouble(pos -> pos.getTotalPnl().doubleValue())
            .sum();
        
        double totalInterest = positionResults.stream()
            .mapToDouble(pos -> pos.getTotalInterest().doubleValue())
            .sum();
        
        double totalDividends = positionResults.stream()
            .mapToDouble(pos -> pos.getTotalDividends().doubleValue())
            .sum();
        
        // Collect all lot-level cash flows for legacy support
        List<CashFlowResponse.CashFlow> allCashFlows = positionResults.stream()
            .flatMap(pos -> pos.getLots().stream())
            .collect(Collectors.toList());
        
        return CashFlowResponse.ContractResult.builder()
            .contractId(contractPosition.getContractId())
            .underlying(contractPosition.getUnderlying())
            .index(contractPosition.getIndex())
            .type(contractPosition.getType() != null ? contractPosition.getType().name() : null)
            .currency(contractPosition.getCurrency())
            .totalPnl(java.math.BigDecimal.valueOf(totalPnl))
            .totalInterest(java.math.BigDecimal.valueOf(totalInterest))
            .totalDividends(java.math.BigDecimal.valueOf(totalDividends))
            .totalCashFlows(java.math.BigDecimal.valueOf(totalPnl + totalInterest + totalDividends))
            .positions(positionResults) // Hierarchical structure
            .cashFlows(allCashFlows) // Legacy support
            .status("SUCCESS")
            .build();
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
        
        // Process lots in parallel using CompletableFuture
        List<CompletableFuture<CashFlowResponse.CashFlow>> lotFutures = contractLots
            .stream()
            .map(lot -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Processing lot: {} in contract: {} on thread: {}", 
                        lot.getLotId(), contract.getContractId(), Thread.currentThread().getName());
                    return createLegacyLotCashFlow(contract, lot, request, marketData, 
                        totalPnl, totalInterest, totalDividends, totalQuantity);
                } catch (Exception e) {
                    log.error("Failed to create cash flow for lot: {} in contract: {}", 
                        lot.getLotId(), contract.getContractId(), e);
                    throw new CalculationException("Failed to create cash flow for lot: " + lot.getLotId(), e);
                }
            }))
            .collect(Collectors.toList());

        // Wait for all lot calculations to complete
        log.debug("Waiting for {} lot calculations to complete for contract: {}", 
            lotFutures.size(), contract.getContractId());

        lotCashFlows = lotFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        log.info("Created {} lot-level cash flows for contract: {}", lotCashFlows.size(), contract.getContractId());
        return lotCashFlows;
    }
    
    /**
     * Create individual legacy lot cash flow record
     */
    private CashFlowResponse.CashFlow createLegacyLotCashFlow(CashFlowRequest.Contract contract,
                                                            CashFlowRequest.Lot lot,
                                                            CashFlowRequest request,
                                                            MarketData marketData,
                                                            double totalPnl,
                                                            double totalInterest,
                                                            double totalDividends,
                                                            double totalQuantity) {
        double lotRatio = lot.getQuantity() / totalQuantity;

        // Create individual cash flow record for this lot
        return CashFlowResponse.CashFlow.builder()
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
            .createdTimestamp(java.time.LocalDateTime.now())
            .build();
    }
    
    /**
     * Create lot-level cash flow records for a specific position within a contract
     * This creates partitions by contract + position combinations
     * Uses parallel processing for lot calculations
     */
    private List<CashFlowResponse.CashFlow> createPositionLotLevelCashFlows(CashFlowRequest.ContractPosition contractPosition, 
                                                                           CashFlowRequest.Position position, 
                                                                           CashFlowRequest request, 
                                                                           MarketData marketData,
                                                                           double totalPnl, 
                                                                           double totalInterest, 
                                                                           double totalDividends) {
        log.info("Creating position-specific lot-level cash flows for contract: {}, position: {}, total lots: {}",
            contractPosition.getContractId(), position.getPositionId(), position.getLots().size());

        if (position.getLots().isEmpty()) {
            log.warn("No lots found for position: {} in contract: {}", position.getPositionId(), contractPosition.getContractId());
            return new ArrayList<>();
        }

        // Calculate per-lot amounts (proportional to quantity)
        double totalQuantity = position.getLots().stream()
            .mapToDouble(CashFlowRequest.Lot::getQuantity)
            .sum();

        // Process lots in parallel using CompletableFuture
        List<CompletableFuture<CashFlowResponse.CashFlow>> lotFutures = position.getLots()
            .stream()
            .map(lot -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Processing lot: {} in position: {} on thread: {}", 
                        lot.getLotId(), position.getPositionId(), Thread.currentThread().getName());
                    return createLotCashFlow(contractPosition, position, lot, request, marketData, 
                        totalPnl, totalInterest, totalDividends, totalQuantity);
                } catch (Exception e) {
                    log.error("Failed to create cash flow for lot: {} in position: {}", 
                        lot.getLotId(), position.getPositionId(), e);
                    throw new CalculationException("Failed to create cash flow for lot: " + lot.getLotId(), e);
                }
            }))
            .collect(Collectors.toList());

        // Wait for all lot calculations to complete
        log.debug("Waiting for {} lot calculations to complete for position: {}", 
            lotFutures.size(), position.getPositionId());

        List<CashFlowResponse.CashFlow> lotCashFlows = lotFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        log.info("Created {} position-specific lot-level cash flows for contract: {}, position: {}", 
            lotCashFlows.size(), contractPosition.getContractId(), position.getPositionId());
        return lotCashFlows;
    }
    
    /**
     * Create individual lot cash flow record
     */
    private CashFlowResponse.CashFlow createLotCashFlow(CashFlowRequest.ContractPosition contractPosition,
                                                       CashFlowRequest.Position position,
                                                       CashFlowRequest.Lot lot,
                                                       CashFlowRequest request,
                                                       MarketData marketData,
                                                       double totalPnl,
                                                       double totalInterest,
                                                       double totalDividends,
                                                       double totalQuantity) {
        double lotRatio = lot.getQuantity() / totalQuantity;

        // Create individual cash flow record for this lot within this position
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId(UUID.randomUUID().toString())
            .requestId(request.getRequestId())
            .contractId(contractPosition.getContractId())
            .positionId(position.getPositionId()) // Position-specific
            .lotId(lot.getLotId())
            .calculationDate(request.getCalculationDate())
            .cashFlowType(contractPosition.getType().name())
            .equityLegAmount(java.math.BigDecimal.valueOf(totalPnl * lotRatio))
            .interestLegAmount(java.math.BigDecimal.valueOf(totalInterest * lotRatio))
            .totalAmount(java.math.BigDecimal.valueOf((totalPnl + totalInterest + totalDividends) * lotRatio))
            .currency(contractPosition.getCurrency())
            .state("REALIZED_UNSETTLED")
            .equityUnrealizedPnl(java.math.BigDecimal.valueOf(totalPnl * lotRatio))
            .equityRealizedPnl(java.math.BigDecimal.ZERO)
            .equityTotalPnl(java.math.BigDecimal.valueOf(totalPnl * lotRatio))
            .equityDividendAmount(java.math.BigDecimal.valueOf(totalDividends * lotRatio))
            .equityWithholdingTax(java.math.BigDecimal.ZERO)
            .equityNetDividend(java.math.BigDecimal.valueOf(totalDividends * lotRatio))
            .interestAccruedAmount(java.math.BigDecimal.valueOf(totalInterest * lotRatio))
            .interestRate(java.math.BigDecimal.valueOf(getInterestRateForPosition(contractPosition, marketData)))
            .interestNotionalAmount(java.math.BigDecimal.valueOf(position.getNotionalAmount() * lotRatio))
            .createdTimestamp(java.time.LocalDateTime.now())
            .build();
    }
    
    /**
     * Get interest rate from market data for a position
     */
    private double getInterestRateForPosition(CashFlowRequest.ContractPosition contractPosition, MarketData marketData) {
        if (marketData.getRate() != null && contractPosition.getIndex().equals(marketData.getRate().getIndex())) {
            return marketData.getRate().getBaseRate();
        }
        return 0.0;
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
