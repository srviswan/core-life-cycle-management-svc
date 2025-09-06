package com.financial.cashflow.service;

import com.financial.cashflow.exception.CashFlowCalculationException;
import com.financial.cashflow.exception.DataPersistenceException;
import com.financial.cashflow.exception.DataRetrievalException;
import com.financial.cashflow.exception.MarketDataException;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.model.MarketData;
import com.financial.cashflow.model.SettlementInstruction;
import com.financial.cashflow.repository.CashFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for Cash Flow Management
 * Uses virtual threads for I/O operations and platform threads for CPU work
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CashFlowService {
    
    private final CalculationEngine calculationEngine;
    private final MarketDataService marketDataService;
    private final CashFlowRepository cashFlowRepository;
    private final CalculationStatusService statusService;
    
    // Thread pool for I/O operations (market data, database)
    private final ExecutorService ioThreadExecutor = Executors.newFixedThreadPool(20);
    
    /**
     * Calculate cash flows synchronously
     */
    public CashFlowResponse calculate(CashFlowRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting calculation for request: {}", request.getRequestId());
        
        try {
            // Load market data using virtual threads for I/O
            MarketData marketData = loadMarketDataAsync(request);
            log.info("Market data loaded in {}ms", System.currentTimeMillis() - startTime);
            
            // Perform calculations using platform threads for CPU work
            CashFlowResponse response = calculationEngine.calculate(request, marketData);
            log.info("Calculation completed in {}ms", System.currentTimeMillis() - startTime);
            
            // Save results
            saveCashFlows(response);
            log.info("Results saved in {}ms", System.currentTimeMillis() - startTime);
            
            return response;
        } catch (Exception e) {
            log.error("Calculation failed for request: {}", request.getRequestId(), e);
            throw new CashFlowCalculationException("Calculation failed", e);
        }
    }
    
    /**
     * Calculate cash flows in real-time (optimized for speed)
     */
    public CashFlowResponse calculateRealTime(CashFlowRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting real-time calculation for request: {}", request.getRequestId());
        
        try {
            // For real-time, use direct execution for speed
            MarketData marketData = marketDataService.loadMarketData(request);
            CashFlowResponse response = calculationEngine.calculateRealTime(request, marketData);
            
            log.info("Real-time calculation completed in {}ms", System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.error("Real-time calculation failed for request: {}", request.getRequestId(), e);
            throw new CashFlowCalculationException("Real-time calculation failed", e);
        }
    }
    
    /**
     * Start historical calculation asynchronously
     */
    public String calculateHistoricalAsync(CashFlowRequest request) {
        String statusId = UUID.randomUUID().toString();
        
        // Start async processing
        CompletableFuture.runAsync(() -> {
            try {
                statusService.updateStatus(statusId, "PROCESSING", 0);
                
                long startTime = System.currentTimeMillis();
                log.info("Starting historical calculation for request: {}", request.getRequestId());
                
                // Load market data
                MarketData marketData = loadMarketDataAsync(request);
                log.info("Market data loaded in {}ms", System.currentTimeMillis() - startTime);
                
                // Perform historical calculation
                CashFlowResponse response = calculationEngine.calculateHistorical(request, marketData);
                log.info("Historical calculation completed in {}ms", System.currentTimeMillis() - startTime);
                
                // Save results
                saveCashFlows(response);
                
                statusService.updateStatus(statusId, "COMPLETED", 100);
                log.info("Historical calculation completed successfully for request: {}", request.getRequestId());
                
            } catch (Exception e) {
                log.error("Historical calculation failed for request: {}", request.getRequestId(), e);
                statusService.updateStatus(statusId, "FAILED", 0, e.getMessage());
            }
        }, ioThreadExecutor);
        
        return statusId;
    }
    
    /**
     * Load market data asynchronously using virtual threads
     */
    private MarketData loadMarketDataAsync(CashFlowRequest request) {
        try {
            return ioThreadExecutor.submit(() -> marketDataService.loadMarketData(request)).get();
        } catch (Exception e) {
            log.error("Failed to load market data", e);
            throw new MarketDataException("Failed to load market data", e);
        }
    }
    
    /**
     * Save cash flows to database
     */
    private void saveCashFlows(CashFlowResponse response) {
        try {
            if (response.getCashFlows() != null && !response.getCashFlows().isEmpty()) {
                cashFlowRepository.saveAll(response.getCashFlows());
            }
        } catch (Exception e) {
            log.error("Failed to save cash flows", e);
            throw new DataPersistenceException("Failed to save cash flows", e);
        }
    }
    
    /**
     * Get cash flows by contract
     */
    public List<CashFlowResponse.CashFlow> getCashFlowsByContract(String contractId, 
                                                                java.time.LocalDate fromDate, 
                                                                java.time.LocalDate toDate, 
                                                                String cashFlowType, 
                                                                String state) {
        try {
            return cashFlowRepository.findByContractIdAndDateRange(contractId, fromDate, toDate, cashFlowType, state);
        } catch (Exception e) {
            log.error("Failed to get cash flows for contract: {}", contractId, e);
            throw new DataRetrievalException("Failed to get cash flows", e);
        }
    }
    
    /**
     * Get pending settlements
     */
    public List<SettlementInstruction> getPendingSettlements(String counterparty, String currency) {
        try {
            return cashFlowRepository.findPendingSettlements(counterparty, currency);
        } catch (Exception e) {
            log.error("Failed to get pending settlements", e);
            throw new DataRetrievalException("Failed to get pending settlements", e);
        }
    }
}
