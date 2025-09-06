package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * P&L Calculator for equity positions
 */
@Component
@Slf4j
public class PnLCalculator {
    
    /**
     * Calculate P&L for a contract
     */
    public double calculatePnL(CashFlowRequest.Contract contract, MarketData marketData) {
        log.debug("Calculating P&L for contract: {}", contract.getContractId());
        
        try {
            // Get current price from market data
            double currentPrice = getCurrentPrice(contract.getUnderlying(), marketData);
            
            // Calculate P&L based on contract type
            double pnl = 0.0;
            
            switch (contract.getType()) {
                case EQUITY_SWAP:
                    pnl = calculateEquitySwapPnL(contract, currentPrice);
                    break;
                case EQUITY_FORWARD:
                    pnl = calculateEquityForwardPnL(contract, currentPrice);
                    break;
                case EQUITY_OPTION:
                    pnl = calculateEquityOptionPnL(contract, currentPrice);
                    break;
                default:
                    log.warn("Unsupported contract type for P&L calculation: {}", contract.getType());
                    pnl = 0.0;
            }
            
            log.debug("P&L calculated for contract {}: {}", contract.getContractId(), pnl);
            return pnl;
            
        } catch (Exception e) {
            log.error("Failed to calculate P&L for contract: {}", contract.getContractId(), e);
            throw new RuntimeException("P&L calculation failed", e);
        }
    }
    
    /**
     * Get current price for underlying
     */
    private double getCurrentPrice(String underlying, MarketData marketData) {
        if (marketData.getPrice() != null && marketData.getPrice().getSymbol().equals(underlying)) {
            return marketData.getPrice().getBasePrice();
        }
        throw new RuntimeException("Price data not found for underlying: " + underlying);
    }
    
    /**
     * Calculate P&L for equity swap
     */
    private double calculateEquitySwapPnL(CashFlowRequest.Contract contract, double currentPrice) {
        // Simplified P&L calculation for equity swap
        // In real implementation, this would be more complex
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        return notional * (currentPrice - 100.0) / 100.0; // Assuming base price of 100
    }
    
    /**
     * Calculate P&L for equity forward
     */
    private double calculateEquityForwardPnL(CashFlowRequest.Contract contract, double currentPrice) {
        // Simplified P&L calculation for equity forward
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        return notional * (currentPrice - 100.0) / 100.0; // Assuming base price of 100
    }
    
    /**
     * Calculate P&L for equity option
     */
    private double calculateEquityOptionPnL(CashFlowRequest.Contract contract, double currentPrice) {
        // Simplified P&L calculation for equity option
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        return notional * (currentPrice - 100.0) / 100.0; // Assuming base price of 100
    }
}
