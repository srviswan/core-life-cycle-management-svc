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
     * Calculate P&L for a contract using lot-based calculations
     */
    public double calculatePnL(CashFlowRequest.Contract contract, MarketData marketData, 
                              LocalDate calculationDate, List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating lot-based P&L for contract: {}", contract.getContractId());
        
        try {
            // Get current price from market data
            double currentPrice = getCurrentPrice(contract.getUnderlying(), marketData);
            
            // Calculate lot-based P&L
            double lotBasedPnL = calculateLotBasedPnL(contract.getContractId(), currentPrice, calculationDate, lots);
            
            // Fall back to contract-based calculation if no lots are available
            double contractBasedPnL = calculateContractBasedPnL(contract, currentPrice);
            
            // Use lot-based P&L if available, otherwise fall back to contract-based
            double effectivePnL = lotBasedPnL != 0.0 ? lotBasedPnL : contractBasedPnL;
            
            log.debug("P&L calculated for contract {}: lot-based={}, contract-based={}, effective={}", 
                     contract.getContractId(), lotBasedPnL, contractBasedPnL, effectivePnL);
            
            return effectivePnL;
            
        } catch (Exception e) {
            log.error("Failed to calculate lot-based P&L for contract: {}", contract.getContractId(), e);
            throw new RuntimeException("P&L calculation failed", e);
        }
    }
    
    /**
     * Calculate P&L for a contract (legacy method for backward compatibility)
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
        // First try multiple securities support
        if (marketData.getPrices() != null && !marketData.getPrices().isEmpty()) {
            for (MarketData.PriceData priceData : marketData.getPrices()) {
                if (priceData.getSymbol().equals(underlying)) {
                    return priceData.getBasePrice();
                }
            }
        }
        
        // Fall back to legacy single price support
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
    
    /**
     * Calculate lot-based P&L: (calculation date notional - cost notional)
     * Only considers lots where cost date is on or before the calculation date
     */
    private double calculateLotBasedPnL(String contractId, double currentPrice, LocalDate calculationDate, 
                                       List<CashFlowRequest.Lot> lots) {
        if (lots == null || lots.isEmpty()) {
            log.debug("No lots provided for contract {}", contractId);
            return 0.0;
        }
        
        // Filter lots for this contract
        List<CashFlowRequest.Lot> contractLots = lots.stream()
                .filter(lot -> contractId.equals(lot.getContractId()))
                .collect(Collectors.toList());
        
        if (contractLots.isEmpty()) {
            log.debug("No lots found for contract {}", contractId);
            return 0.0;
        }
        
        // Calculate P&L for lots with cost date on or before calculation date
        double totalPnL = contractLots.stream()
                .filter(lot -> isLotCostDateValid(lot, calculationDate))
                .filter(lot -> isLotActive(lot))
                .mapToDouble(lot -> {
                    double quantity = lot.getQuantity() != null ? lot.getQuantity() : 0.0;
                    double costPrice = lot.getCostPrice() != null ? lot.getCostPrice() : 0.0;
                    
                    // P&L = quantity * (current price - cost price)
                    double lotPnL = quantity * (currentPrice - costPrice);
                    
                    log.debug("Lot {} P&L: quantity={}, currentPrice={}, costPrice={}, pnl={}", 
                             lot.getLotId(), quantity, currentPrice, costPrice, lotPnL);
                    
                    return lotPnL;
                })
                .sum();
        
        log.debug("Total lot-based P&L for contract {}: {}", contractId, totalPnL);
        return totalPnL;
    }
    
    /**
     * Calculate contract-based P&L (legacy method)
     */
    private double calculateContractBasedPnL(CashFlowRequest.Contract contract, double currentPrice) {
        // Simplified P&L calculation for contract
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        return notional * (currentPrice - 100.0) / 100.0; // Assuming base price of 100
    }
    
    /**
     * Check if lot's cost date is on or before the calculation date
     */
    private boolean isLotCostDateValid(CashFlowRequest.Lot lot, LocalDate calculationDate) {
        if (lot.getCostDate() == null) {
            log.debug("Lot {} has no cost date, treating as invalid", lot.getLotId());
            return false;
        }
        
        boolean isValid = !lot.getCostDate().isAfter(calculationDate);
        log.debug("Lot {} cost date {} vs calculation date {}: valid={}", 
                 lot.getLotId(), lot.getCostDate(), calculationDate, isValid);
        
        return isValid;
    }
    
    /**
     * Check if a lot is active
     */
    private boolean isLotActive(CashFlowRequest.Lot lot) {
        boolean isActive = lot.getStatus() == null || 
                          lot.getStatus() == CashFlowRequest.Lot.LotStatus.ACTIVE;
        
        log.debug("Lot {} status {}: active={}", lot.getLotId(), lot.getStatus(), isActive);
        
        return isActive;
    }
    
    /**
     * Calculate P&L for a specific position within a contract
     * This creates partitions by contract + position combinations
     */
    public double calculatePnLForPosition(CashFlowRequest.ContractPosition contractPosition, 
                                        CashFlowRequest.Position position, 
                                        MarketData marketData, 
                                        LocalDate calculationDate) {
        log.debug("Calculating position-based P&L for contract: {}, position: {}, type: {}", 
            contractPosition.getContractId(), position.getPositionId(), position.getType());
        
        try {
            // Check position type - only calculate P&L for equity positions
            if (position.getType() != null && position.getType() != CashFlowRequest.Position.PositionType.EQUITY_LEG) {
                log.debug("Skipping P&L calculation for non-equity position: {} (type: {})", 
                    position.getPositionId(), position.getType());
                return 0.0; // Non-equity positions don't have P&L
            }
            
            // Get current price from market data
            double currentPrice = getCurrentPrice(position.getUnderlying(), marketData);
            
            // Calculate position-specific P&L using position lots
            double positionBasedPnL = calculateLotBasedPnL(position.getPositionId(), currentPrice, calculationDate, position.getLots());
            
            // Fall back to position-based calculation if no lots are available
            double fallbackPnL = calculatePositionBasedPnL(position, currentPrice);
            
            // Use lot-based P&L if available, otherwise fall back to position-based
            double effectivePnL = positionBasedPnL != 0.0 ? positionBasedPnL : fallbackPnL;
            
            log.debug("Position P&L calculated for contract {} + position {}: lot-based={}, position-based={}, effective={}", 
                     contractPosition.getContractId(), position.getPositionId(), positionBasedPnL, fallbackPnL, effectivePnL);
            
            return effectivePnL;
            
        } catch (Exception e) {
            log.error("Failed to calculate position-based P&L for contract: {} + position: {}", 
                contractPosition.getContractId(), position.getPositionId(), e);
            throw new RuntimeException("Position P&L calculation failed", e);
        }
    }
    
    /**
     * Calculate position-based P&L (fallback when no lots available)
     */
    private double calculatePositionBasedPnL(CashFlowRequest.Position position, double currentPrice) {
        // Simple position-based calculation
        // This would be enhanced based on position type and business rules
        double notional = position.getNotionalAmount() != null ? position.getNotionalAmount() : 0.0;
        double costPrice = 100.0; // Default cost price - would come from position data
        
        return (currentPrice - costPrice) * (notional / currentPrice);
    }
}
