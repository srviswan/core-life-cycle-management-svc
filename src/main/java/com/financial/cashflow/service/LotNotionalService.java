package com.financial.cashflow.service;

import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating interest-bearing notional based on lots and settlement dates
 */
@Service
@Slf4j
public class LotNotionalService {
    
    /**
     * Calculate the interest-bearing notional for a contract based on settled lots
     * on a specific calculation date
     */
    public double calculateInterestBearingNotional(String contractId, LocalDate calculationDate, 
                                                  List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating interest-bearing notional for contract {} on date {}", 
                  contractId, calculationDate);
        
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
        
        // Calculate total settled quantity for this calculation date
        double totalSettledQuantity = contractLots.stream()
                .filter(lot -> isLotSettledOnDate(lot, calculationDate))
                .filter(lot -> isLotActive(lot))
                .mapToDouble(lot -> {
                    double quantity = lot.getQuantity() != null ? lot.getQuantity() : 0.0;
                    double costPrice = lot.getCostPrice() != null ? lot.getCostPrice() : 0.0;
                    double notionalValue = quantity * costPrice;
                    
                    log.debug("Lot {} settled on {}: quantity={}, costPrice={}, notionalValue={}", 
                             lot.getLotId(), calculationDate, quantity, costPrice, notionalValue);
                    
                    return notionalValue;
                })
                .sum();
        
        log.debug("Total interest-bearing notional for contract {} on {}: {}", 
                  contractId, calculationDate, totalSettledQuantity);
        
        return totalSettledQuantity;
    }
    
    /**
     * Calculate the interest-bearing notional for a contract based on lots settled
     * within a date range
     */
    public double calculateInterestBearingNotionalForPeriod(String contractId, 
                                                           LocalDate startDate, 
                                                           LocalDate endDate,
                                                           List<CashFlowRequest.Lot> lots) {
        log.debug("Calculating interest-bearing notional for contract {} from {} to {}", 
                  contractId, startDate, endDate);
        
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
        
        // Calculate total settled quantity within the period
        double totalSettledQuantity = contractLots.stream()
                .filter(lot -> isLotSettledInPeriod(lot, startDate, endDate))
                .filter(lot -> isLotActive(lot))
                .mapToDouble(lot -> {
                    double quantity = lot.getQuantity() != null ? lot.getQuantity() : 0.0;
                    double costPrice = lot.getCostPrice() != null ? lot.getCostPrice() : 0.0;
                    double notionalValue = quantity * costPrice;
                    
                    log.debug("Lot {} settled in period {} to {}: quantity={}, costPrice={}, notionalValue={}", 
                             lot.getLotId(), startDate, endDate, quantity, costPrice, notionalValue);
                    
                    return notionalValue;
                })
                .sum();
        
        log.debug("Total interest-bearing notional for contract {} from {} to {}: {}", 
                  contractId, startDate, endDate, totalSettledQuantity);
        
        return totalSettledQuantity;
    }
    
    /**
     * Check if a lot is settled on a specific date
     */
    private boolean isLotSettledOnDate(CashFlowRequest.Lot lot, LocalDate calculationDate) {
        if (lot.getSettlementDate() == null) {
            log.debug("Lot {} has no settlement date, treating as not settled", lot.getLotId());
            return false;
        }
        
        boolean isSettled = !lot.getSettlementDate().isAfter(calculationDate);
        log.debug("Lot {} settlement date {} vs calculation date {}: settled={}", 
                 lot.getLotId(), lot.getSettlementDate(), calculationDate, isSettled);
        
        return isSettled;
    }
    
    /**
     * Check if a lot is settled within a date period
     */
    private boolean isLotSettledInPeriod(CashFlowRequest.Lot lot, LocalDate startDate, LocalDate endDate) {
        if (lot.getSettlementDate() == null) {
            log.debug("Lot {} has no settlement date, treating as not settled", lot.getLotId());
            return false;
        }
        
        boolean isSettledInPeriod = !lot.getSettlementDate().isBefore(startDate) && 
                                   !lot.getSettlementDate().isAfter(endDate);
        
        log.debug("Lot {} settlement date {} vs period {} to {}: settled in period={}", 
                 lot.getLotId(), lot.getSettlementDate(), startDate, endDate, isSettledInPeriod);
        
        return isSettledInPeriod;
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
