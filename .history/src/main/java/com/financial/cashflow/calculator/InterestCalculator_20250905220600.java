package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import com.financial.cashflow.service.LotNotionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

/**
 * Interest Calculator for interest-bearing instruments
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterestCalculator {
    
    private final LotNotionalService lotNotionalService;
    
    /**
     * Calculate interest for a contract
     */
    public double calculateInterest(CashFlowRequest.Contract contract, MarketData marketData) {
        log.debug("Calculating interest for contract: {}", contract.getContractId());
        
        try {
            // Get current rate from market data
            double currentRate = getCurrentRate(contract.getIndex(), marketData);
            
            // Calculate interest based on contract type
            double interest = 0.0;
            
            switch (contract.getType()) {
                case INTEREST_RATE_SWAP:
                    interest = calculateInterestRateSwapInterest(contract, currentRate);
                    break;
                case BOND:
                    interest = calculateBondInterest(contract, currentRate);
                    break;
                case EQUITY_SWAP:
                    interest = calculateEquitySwapInterest(contract, currentRate);
                    break;
                default:
                    log.debug("No interest calculation for contract type: {}", contract.getType());
                    interest = 0.0;
            }
            
            log.debug("Interest calculated for contract {}: {}", contract.getContractId(), interest);
            return interest;
            
        } catch (Exception e) {
            log.error("Failed to calculate interest for contract: {}", contract.getContractId(), e);
            throw new RuntimeException("Interest calculation failed", e);
        }
    }
    
    /**
     * Get current rate for index
     */
    private double getCurrentRate(String index, MarketData marketData) {
        if (marketData.getRate() != null && marketData.getRate().getIndex().equals(index)) {
            return marketData.getRate().getBaseRate();
        }
        throw new RuntimeException("Rate data not found for index: " + index);
    }
    
    /**
     * Calculate interest for interest rate swap
     */
    private double calculateInterestRateSwapInterest(CashFlowRequest.Contract contract, double currentRate) {
        // Simplified interest calculation for interest rate swap
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        long days = ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate());
        return notional * currentRate * days / 365.0;
    }
    
    /**
     * Calculate interest for bond
     */
    private double calculateBondInterest(CashFlowRequest.Contract contract, double currentRate) {
        // Simplified interest calculation for bond
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        long days = ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate());
        return notional * currentRate * days / 365.0;
    }
    
    /**
     * Calculate interest for equity swap
     */
    private double calculateEquitySwapInterest(CashFlowRequest.Contract contract, double currentRate) {
        // Simplified interest calculation for equity swap
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        long days = ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate());
        return notional * currentRate * days / 365.0;
    }
}
