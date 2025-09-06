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
        if (dividend.getExDate() == null) {
            return false; // Null exDate is invalid
        }
        return !dividend.getExDate().isBefore(startDate) && !dividend.getExDate().isAfter(endDate);
    }
    
    /**
     * Calculate lot-based dividends: Only dividends with exDate on or before calculation date
     * and paymentDate on or before calculation date (if paymentDate is provided)
     */
    private double calculateLotBasedDividends(String contractId, 
                                             List<MarketData.DividendData.Dividend> dividends, 
                                             LocalDate calculationDate, 
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
        
        // Calculate total quantity from active lots with valid cost dates
        double totalQuantity = contractLots.stream()
                .filter(lot -> isLotCostDateValid(lot, calculationDate))
                .filter(lot -> isLotActive(lot))
                .mapToDouble(lot -> lot.getQuantity() != null ? lot.getQuantity() : 0.0)
                .sum();
        
        if (totalQuantity == 0.0) {
            log.debug("No valid lots found for contract {} on calculation date {}", contractId, calculationDate);
            return 0.0;
        }
        
        // Calculate dividends for dividends with valid exDate and paymentDate
        double totalDividends = dividends.stream()
                .filter(dividend -> isDividendExDateValid(dividend, calculationDate))
                .filter(dividend -> isDividendPaymentDateValid(dividend, calculationDate))
                .mapToDouble(dividend -> {
                    double dividendPerShare = dividend.getAmount() != null ? dividend.getAmount() : 0.0;
                    double totalDividendAmount = totalQuantity * dividendPerShare;
                    
                    // Apply withholding tax treatment
                    double netDividendAmount = applyWithholdingTax(dividend, totalDividendAmount);
                    
                    log.debug("Dividend {}: exDate={}, paymentDate={}, amount={}, quantity={}, gross={}, net={}, withholding={}", 
                             dividend.getExDate(), dividend.getExDate(), dividend.getPaymentDate(), 
                             dividendPerShare, totalQuantity, totalDividendAmount, netDividendAmount,
                             dividend.getWithholdingTaxRate());
                    
                    return netDividendAmount;
                })
                .sum();
        
        log.debug("Total lot-based dividends for contract {}: {}", contractId, totalDividends);
        return totalDividends;
    }
    
    /**
     * Calculate contract-based dividends (legacy method)
     */
    private double calculateContractBasedDividends(CashFlowRequest.Contract contract, 
                                                  List<MarketData.DividendData.Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> {
                double grossDividendAmount = notional * dividend.getAmount() / 1000000.0; // Scale by notional
                return applyWithholdingTax(dividend, grossDividendAmount);
            })
            .sum();
    }
    
    /**
     * Check if dividend's exDate is on or before the calculation date
     */
    private boolean isDividendExDateValid(MarketData.DividendData.Dividend dividend, LocalDate calculationDate) {
        if (dividend.getExDate() == null) {
            log.debug("Dividend has no exDate, treating as invalid");
            return false;
        }
        
        boolean isValid = !dividend.getExDate().isAfter(calculationDate);
        log.debug("Dividend exDate {} vs calculation date {}: valid={}", 
                 dividend.getExDate(), calculationDate, isValid);
        
        return isValid;
    }
    
    /**
     * Check if dividend's paymentDate is on or before the calculation date (if paymentDate is provided)
     */
    private boolean isDividendPaymentDateValid(MarketData.DividendData.Dividend dividend, LocalDate calculationDate) {
        if (dividend.getPaymentDate() == null) {
            log.debug("Dividend has no paymentDate, treating as valid (paymentDate not required)");
            return true; // paymentDate is optional, so if not provided, consider it valid
        }
        
        boolean isValid = !dividend.getPaymentDate().isAfter(calculationDate);
        log.debug("Dividend paymentDate {} vs calculation date {}: valid={}", 
                 dividend.getPaymentDate(), calculationDate, isValid);
        
        return isValid;
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
}
