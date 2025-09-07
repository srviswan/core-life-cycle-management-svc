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
     * Calculate interest for a contract using lot-based notional
     */
    public double calculateInterest(CashFlowRequest.Contract contract, MarketData marketData, 
                                   java.time.LocalDate calculationDate, java.util.List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating interest for contract: {} with lot-based notional", contract.getContractId());
        
        try {
            // Get current rate from market data
            double currentRate = getCurrentRate(contract.getIndex(), marketData);
            
            // Calculate lot-based notional
            double lotBasedNotional = lotNotionalService.calculateInterestBearingNotional(
                contract.getContractId(), calculationDate, lots);
            
            // Fall back to contract notional if no lots are settled
            double effectiveNotional = lotBasedNotional > 0.0 ? lotBasedNotional : 
                (contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0);
            
            log.debug("Using notional: lot-based={}, contract={}, effective={}", 
                     lotBasedNotional, contract.getNotionalAmount(), effectiveNotional);
            
            // Calculate interest based on contract type
            double interest = 0.0;
            
            switch (contract.getType()) {
                case INTEREST_RATE_SWAP:
                    interest = calculateInterestRateSwapInterestWithNotional(effectiveNotional, currentRate, 
                                                                           contract.getStartDate(), contract.getEndDate());
                    break;
                case BOND:
                    interest = calculateBondInterestWithNotional(effectiveNotional, currentRate, 
                                                               contract.getStartDate(), contract.getEndDate());
                    break;
                case EQUITY_SWAP:
                    interest = calculateEquitySwapInterestWithNotional(effectiveNotional, currentRate, 
                                                                     contract.getStartDate(), contract.getEndDate());
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
     * Calculate interest for a contract (legacy method for backward compatibility)
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
    
    /**
     * Calculate interest for interest rate swap with explicit notional
     */
    private double calculateInterestRateSwapInterestWithNotional(double notional, double currentRate, 
                                                               java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Interest calculation for interest rate swap with lot-based notional
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return notional * currentRate * days / 365.0;
    }
    
    /**
     * Calculate interest for bond with explicit notional
     */
    private double calculateBondInterestWithNotional(double notional, double currentRate, 
                                                   java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Interest calculation for bond with lot-based notional
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return notional * currentRate * days / 365.0;
    }
    
    /**
     * Calculate interest for equity swap with explicit notional
     */
    private double calculateEquitySwapInterestWithNotional(double notional, double currentRate, 
                                                         java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Interest calculation for equity swap with lot-based notional
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return notional * currentRate * days / 365.0;
    }
    
    /**
     * Calculate interest for a specific position within a contract
     * This creates partitions by contract + position combinations
     */
    public double calculateInterestForPosition(CashFlowRequest.ContractPosition contractPosition, 
                                             CashFlowRequest.Position position, 
                                             MarketData marketData, 
                                             java.time.LocalDate calculationDate) {
        log.debug("Calculating position-based interest for contract: {}, position: {}", 
            contractPosition.getContractId(), position.getPositionId());
        
        try {
            // Get current rate from market data
            double currentRate = getCurrentRate(contractPosition.getIndex(), marketData);
            
            // Calculate position-specific notional using position lots
            double positionBasedNotional = lotNotionalService.calculateInterestBearingNotional(
                position.getPositionId(), calculationDate, position.getLots());
            
            // Fall back to position notional if no lots are settled
            double effectiveNotional = positionBasedNotional > 0.0 ? positionBasedNotional : 
                (position.getNotionalAmount() != null ? position.getNotionalAmount() : 1000000.0);
            
            log.debug("Using position notional: lot-based={}, position={}, effective={}", 
                     positionBasedNotional, position.getNotionalAmount(), effectiveNotional);
            
            // Calculate interest based on contract type
            double interest = 0.0;
            
            switch (contractPosition.getType()) {
                case INTEREST_RATE_SWAP:
                    interest = calculateInterestRateSwapInterestWithNotional(effectiveNotional, currentRate, 
                                                                           contractPosition.getStartDate(), contractPosition.getEndDate());
                    break;
                case EQUITY_SWAP:
                    interest = calculateEquitySwapInterestWithNotional(effectiveNotional, currentRate, 
                                                                     contractPosition.getStartDate(), contractPosition.getEndDate());
                    break;
                default:
                    log.warn("Unsupported contract type for interest calculation: {}", contractPosition.getType());
                    break;
            }
            
            log.debug("Position interest calculated for contract {} + position {}: notional={}, rate={}, interest={}", 
                     contractPosition.getContractId(), position.getPositionId(), effectiveNotional, currentRate, interest);
            
            return interest;
            
        } catch (Exception e) {
            log.error("Failed to calculate position-based interest for contract: {} + position: {}", 
                contractPosition.getContractId(), position.getPositionId(), e);
            throw new RuntimeException("Position interest calculation failed", e);
        }
    }
}
