package com.financial.cashflow.service;

import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating PnL based on lots and their cost dates
 * PnL = (calculation date notional - cost notional) for lots with cost date <= calculation date
 */
@Service
@Slf4j
public class LotPnLService {
    
    /**
     * Calculate PnL for a contract based on lots with cost dates on or before calculation date
     */
    public double calculateLotBasedPnL(String contractId, LocalDate calculationDate, 
                                      double currentPrice, List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating lot-based PnL for contract {} on date {} with current price {}", 
                  contractId, calculationDate, currentPrice);
        
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
        
        // Calculate PnL for lots with cost date on or before calculation date
        double totalPnL = contractLots.stream()
                .filter(lot -> isLotCostDateOnOrBefore(lot, calculationDate))
                .filter(lot -> isLotActive(lot))
                .mapToDouble(lot -> {
                    double quantity = lot.getQuantity() != null ? lot.getQuantity() : 0.0;
                    double costPrice = lot.getCostPrice() != null ? lot.getCostPrice() : 0.0;
                    
                    // PnL = (current price - cost price) * quantity
                    double lotPnL = (currentPrice - costPrice) * quantity;
                    
                    log.debug("Lot {} PnL calculation: quantity={}, costPrice={}, currentPrice={}, pnl={}", 
                             lot.getLotId(), quantity, costPrice, currentPrice, lotPnL);
                    
                    return lotPnL;
                })
                .sum();
        
        log.debug("Total lot-based PnL for contract {} on {}: {}", 
                  contractId, calculationDate, totalPnL);
        
        return totalPnL;
    }
    
    /**
     * Calculate PnL for a contract based on lots with cost dates within a period
     */
    public double calculateLotBasedPnLForPeriod(String contractId, LocalDate startDate, LocalDate endDate,
                                               double currentPrice, List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating lot-based PnL for contract {} from {} to {} with current price {}", 
                  contractId, startDate, endDate, currentPrice);
        
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
        
        // Calculate PnL for lots with cost date within the period
        double totalPnL = contractLots.stream()
                .filter(lot -> isLotCostDateInPeriod(lot, startDate, endDate))
                .filter(lot -> isLotActive(lot))
                .mapToDouble(lot -> {
                    double quantity = lot.getQuantity() != null ? lot.getQuantity() : 0.0;
                    double costPrice = lot.getCostPrice() != null ? lot.getCostPrice() : 0.0;
                    
                    // PnL = (current price - cost price) * quantity
                    double lotPnL = (currentPrice - costPrice) * quantity;
                    
                    log.debug("Lot {} PnL calculation for period: quantity={}, costPrice={}, currentPrice={}, pnl={}", 
                             lot.getLotId(), quantity, costPrice, currentPrice, lotPnL);
                    
                    return lotPnL;
                })
                .sum();
        
        log.debug("Total lot-based PnL for contract {} from {} to {}: {}", 
                  contractId, startDate, endDate, totalPnL);
        
        return totalPnL;
    }
    
    /**
     * Check if a lot's cost date is on or before the calculation date
     */
    private boolean isLotCostDateOnOrBefore(CashFlowRequest.Lot lot, LocalDate calculationDate) {
        if (lot.getCostDate() == null) {
            log.debug("Lot {} has no cost date, treating as not eligible for PnL calculation", lot.getLotId());
            return false;
        }
        
        boolean isEligible = !lot.getCostDate().isAfter(calculationDate);
        log.debug("Lot {} cost date {} vs calculation date {}: eligible={}", 
                 lot.getLotId(), lot.getCostDate(), calculationDate, isEligible);
        
        return isEligible;
    }
    
    /**
     * Check if a lot's cost date is within a period
     */
    private boolean isLotCostDateInPeriod(CashFlowRequest.Lot lot, LocalDate startDate, LocalDate endDate) {
        if (lot.getCostDate() == null) {
            log.debug("Lot {} has no cost date, treating as not eligible for PnL calculation", lot.getLotId());
            return false;
        }
        
        boolean isEligible = !lot.getCostDate().isBefore(startDate) && !lot.getCostDate().isAfter(endDate);
        log.debug("Lot {} cost date {} vs period {} to {}: eligible={}", 
                 lot.getLotId(), lot.getCostDate(), startDate, endDate, isEligible);
        
        return isEligible;
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
