package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dividend Calculator for equity positions
 */
@Component
@Slf4j
public class DividendCalculator {
    
    /**
     * Calculate dividends for a contract using lot-based calculations
     */
    public double calculateDividends(CashFlowRequest.Contract contract, MarketData marketData, 
                                    LocalDate calculationDate, List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating lot-based dividends for contract: {}", contract.getContractId());
        
        try {
            // Get dividend data from market data
            List<MarketData.DividendData.Dividend> dividends = getDividends(contract.getUnderlying(), marketData);
            
            // Calculate lot-based dividends
            double lotBasedDividends = calculateLotBasedDividends(contract.getContractId(), dividends, calculationDate, lots);
            
            // Fall back to contract-based calculation if no lots are available
            double contractBasedDividends = calculateContractBasedDividends(contract, dividends);
            
            // Use lot-based dividends if available, otherwise fall back to contract-based
            double effectiveDividends = lotBasedDividends != 0.0 ? lotBasedDividends : contractBasedDividends;
            
            log.debug("Dividends calculated for contract {}: lot-based={}, contract-based={}, effective={}", 
                     contract.getContractId(), lotBasedDividends, contractBasedDividends, effectiveDividends);
            
            return effectiveDividends;
            
        } catch (Exception e) {
            log.error("Failed to calculate lot-based dividends for contract: {}", contract.getContractId(), e);
            throw new RuntimeException("Dividend calculation failed", e);
        }
    }
    
    /**
     * Calculate dividends for a contract (legacy method for backward compatibility)
     */
    public double calculateDividends(CashFlowRequest.Contract contract, MarketData marketData) {
        log.debug("Calculating dividends for contract: {}", contract.getContractId());
        
        try {
            // Get dividend data from market data
            List<MarketData.DividendData.Dividend> dividends = getDividends(contract.getUnderlying(), marketData);
            
            // Calculate dividends based on contract type
            double totalDividends = 0.0;
            
            switch (contract.getType()) {
                case EQUITY_SWAP:
                    totalDividends = calculateEquitySwapDividends(contract, dividends);
                    break;
                case EQUITY_FORWARD:
                    totalDividends = calculateEquityForwardDividends(contract, dividends);
                    break;
                case EQUITY_OPTION:
                    totalDividends = calculateEquityOptionDividends(contract, dividends);
                    break;
                default:
                    log.debug("No dividend calculation for contract type: {}", contract.getType());
                    totalDividends = 0.0;
            }
            
            log.debug("Dividends calculated for contract {}: {}", contract.getContractId(), totalDividends);
            return totalDividends;
            
        } catch (Exception e) {
            log.error("Failed to calculate dividends for contract: {}", contract.getContractId(), e);
            throw new RuntimeException("Dividend calculation failed", e);
        }
    }
    
    /**
     * Get dividends for underlying
     */
    private List<MarketData.DividendData.Dividend> getDividends(String underlying, MarketData marketData) {
        if (marketData.getDividends() != null && marketData.getDividends().getSymbol().equals(underlying)) {
            return marketData.getDividends().getDividends();
        }
        throw new RuntimeException("Dividend data not found for underlying: " + underlying);
    }
    
    /**
     * Calculate dividends for equity swap
     */
    private double calculateEquitySwapDividends(CashFlowRequest.Contract contract, 
                                               List<MarketData.DividendData.Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> notional * dividend.getAmount() / 1000000.0) // Scale by notional
            .sum();
    }
    
    /**
     * Calculate dividends for equity forward
     */
    private double calculateEquityForwardDividends(CashFlowRequest.Contract contract, 
                                                  List<MarketData.DividendData.Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> notional * dividend.getAmount() / 1000000.0) // Scale by notional
            .sum();
    }
    
    /**
     * Calculate dividends for equity option
     */
    private double calculateEquityOptionDividends(CashFlowRequest.Contract contract, 
                                                  List<MarketData.DividendData.Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> notional * dividend.getAmount() / 1000000.0) // Scale by notional
            .sum();
    }
    
    /**
     * Check if dividend is within contract period
     */
    private boolean isDividendInPeriod(MarketData.DividendData.Dividend dividend, 
                                      LocalDate startDate, 
                                      LocalDate endDate) {
        return !dividend.getExDate().isBefore(startDate) && !dividend.getExDate().isAfter(endDate);
    }
}
