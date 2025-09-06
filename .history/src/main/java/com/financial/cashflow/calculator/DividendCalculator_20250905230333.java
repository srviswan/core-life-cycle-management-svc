package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import com.financial.cashflow.model.Dividend;
import com.financial.cashflow.model.WithholdingTaxInfo;
import com.financial.cashflow.model.DividendCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
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
            List<Dividend> dividends = getDividends(contract.getUnderlying(), marketData);
            
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
            List<Dividend> dividends = getDividends(contract.getUnderlying(), marketData);
            
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
    private List<Dividend> getDividends(String underlying, MarketData marketData) {
        if (marketData.getDividends() != null && marketData.getDividends().getSymbol().equals(underlying)) {
            return marketData.getDividends().getDividends();
        }
        throw new RuntimeException("Dividend data not found for underlying: " + underlying);
    }
    
    /**
     * Calculate dividends for equity swap
     */
    private double calculateEquitySwapDividends(CashFlowRequest.Contract contract, 
                                               List<Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> {
                double grossAmount = notional * dividend.getAmount() / 1000000.0;
                return applyWithholdingTax(grossAmount, dividend.getWithholdingTaxRate(), dividend.getWithholdingTreatment());
            })
            .sum();
    }
    
    /**
     * Calculate dividends for equity forward
     */
    private double calculateEquityForwardDividends(CashFlowRequest.Contract contract, 
                                                  List<Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> {
                double grossAmount = notional * dividend.getAmount() / 1000000.0;
                return applyWithholdingTax(grossAmount, dividend.getWithholdingTaxRate(), dividend.getWithholdingTreatment());
            })
            .sum();
    }
    
    /**
     * Calculate dividends for equity option
     */
    private double calculateEquityOptionDividends(CashFlowRequest.Contract contract, 
                                                 List<Dividend> dividends) {
        // Calculate dividends within contract period
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> {
                double grossAmount = notional * dividend.getAmount() / 1000000.0;
                return applyWithholdingTax(grossAmount, dividend.getWithholdingTaxRate(), dividend.getWithholdingTreatment());
            })
            .sum();
    }
    
    /**
     * Check if dividend is within contract period
     */
    private boolean isDividendInPeriod(Dividend dividend, 
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
                                             List<Dividend> dividends, 
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
                                                  List<Dividend> dividends) {
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
    private boolean isDividendExDateValid(Dividend dividend, LocalDate calculationDate) {
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
    private boolean isDividendPaymentDateValid(Dividend dividend, LocalDate calculationDate) {
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
    
    /**
     * Apply withholding tax treatment to dividend amount
     */
    private double applyWithholdingTax(Dividend dividend, double grossAmount) {
        if (dividend.getWithholdingTaxRate() == null || dividend.getWithholdingTaxRate() == 0.0) {
            log.debug("No withholding tax applied: rate={}", dividend.getWithholdingTaxRate());
            return grossAmount;
        }
        
        if (dividend.getWithholdingTreatment() == null) {
            log.debug("No withholding treatment specified, defaulting to GROSS_UP");
            return applyGrossUpTreatment(dividend.getWithholdingTaxRate(), grossAmount);
        }
        
        double netAmount = 0.0;
        switch (dividend.getWithholdingTreatment()) {
            case GROSS_UP:
                netAmount = applyGrossUpTreatment(dividend.getWithholdingTaxRate(), grossAmount);
                break;
            case NET_AMOUNT:
                netAmount = grossAmount; // Amount is already net
                break;
            case NO_WITHHOLDING:
                netAmount = grossAmount; // No tax deducted
                break;
            case TAX_CREDIT:
                netAmount = applyTaxCreditTreatment(dividend.getWithholdingTaxRate(), grossAmount);
                break;
            default:
                log.warn("Unknown withholding treatment: {}, defaulting to GROSS_UP", dividend.getWithholdingTreatment());
                netAmount = applyGrossUpTreatment(dividend.getWithholdingTaxRate(), grossAmount);
        }
        
        log.debug("Withholding tax applied: treatment={}, rate={}%, gross={}, net={}", 
                 dividend.getWithholdingTreatment(), dividend.getWithholdingTaxRate(), grossAmount, netAmount);
        
        return netAmount;
    }
    
    /**
     * Apply gross-up withholding treatment: dividend amount is gross, tax is deducted
     */
    private double applyGrossUpTreatment(double withholdingRate, double grossAmount) {
        double withholdingTax = grossAmount * (withholdingRate / 100.0);
        double netAmount = grossAmount - withholdingTax;
        log.debug("Gross-up treatment: gross={}, withholding={}, net={}", grossAmount, withholdingTax, netAmount);
        return netAmount;
    }
    
    /**
     * Apply tax credit withholding treatment: dividend amount is gross, but tax can be claimed as credit
     */
    private double applyTaxCreditTreatment(double withholdingRate, double grossAmount) {
        // For tax credit treatment, we return the gross amount as the investor can claim the tax credit
        // The withholding tax is still deducted but can be recovered through tax credits
        double withholdingTax = grossAmount * (withholdingRate / 100.0);
        log.debug("Tax credit treatment: gross={}, withholding={} (recoverable)", grossAmount, withholdingTax);
        return grossAmount; // Return gross amount as tax credit can be claimed
    }
    
    /**
     * Calculate dividends with detailed withholding tax information
     */
    public DividendCalculationResult calculateDividendsWithDetails(CashFlowRequest.Contract contract, 
                                                                   MarketData marketData,
                                                                   LocalDate calculationDate,
                                                                   List<CashFlowRequest.Lot> lots) {
        try {
            log.debug("Calculating dividends with details for contract: {}", contract.getContractId());
            
            List<Dividend> dividends = getDividends(contract.getUnderlying(), marketData);
            List<WithholdingTaxInfo> withholdingTaxDetails = new ArrayList<>();
            
            double totalDividendAmount = 0.0;
            
            if (lots != null && !lots.isEmpty()) {
                // Use lot-based calculation
                totalDividendAmount = calculateLotBasedDividendsWithDetails(contract, dividends, calculationDate, lots, withholdingTaxDetails);
            } else {
                // Use contract-based calculation
                totalDividendAmount = calculateContractBasedDividendsWithDetails(contract, dividends, withholdingTaxDetails);
            }
            
            return DividendCalculationResult.builder()
                    .totalDividendAmount(totalDividendAmount)
                    .withholdingTaxDetails(withholdingTaxDetails)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to calculate dividends with details for contract: {}", contract.getContractId(), e);
            throw new RuntimeException("Dividend calculation failed", e);
        }
    }
    
    /**
     * Calculate lot-based dividends with detailed withholding tax information
     */
    private double calculateLotBasedDividendsWithDetails(CashFlowRequest.Contract contract,
                                                        List<Dividend> dividends,
                                                        LocalDate calculationDate,
                                                        List<CashFlowRequest.Lot> lots,
                                                        List<WithholdingTaxInfo> withholdingTaxDetails) {
        
        // Filter lots for the specific contract and those with valid cost dates
        List<CashFlowRequest.Lot> validLots = lots.stream()
                .filter(lot -> contract.getContractId().equals(lot.getContractId()))
                .filter(lot -> isLotCostDateValid(lot, calculationDate))
                .filter(lot -> isLotActive(lot))
                .collect(Collectors.toList());
        
        if (validLots.isEmpty()) {
            log.debug("No valid lots found for contract {} on calculation date {}", contract.getContractId(), calculationDate);
            return 0.0;
        }
        
        // Calculate total quantity from valid lots
        double totalQuantity = validLots.stream()
                .mapToDouble(lot -> lot.getQuantity() != null ? lot.getQuantity() : 0.0)
                .sum();
        
        log.debug("Total quantity for contract {}: {}", contract.getContractId(), totalQuantity);
        
        // Calculate dividends for dividends with valid exDate and paymentDate
        double totalDividends = dividends.stream()
                .filter(dividend -> isDividendExDateValid(dividend, calculationDate))
                .filter(dividend -> isDividendPaymentDateValid(dividend, calculationDate))
                .mapToDouble(dividend -> {
                    double dividendPerShare = dividend.getAmount() != null ? dividend.getAmount() : 0.0;
                    double grossDividendAmount = totalQuantity * dividendPerShare;
                    
                    // Apply withholding tax treatment
                    double netDividendAmount = applyWithholdingTax(dividend, grossDividendAmount);
                    
                    // Create withholding tax info
                    WithholdingTaxInfo withholdingInfo = createWithholdingTaxInfo(
                            contract, dividend, grossDividendAmount, netDividendAmount, 
                            calculationDate, "LOT_BASED", null);
                    withholdingTaxDetails.add(withholdingInfo);
                    
                    log.debug("Dividend {}: exDate={}, paymentDate={}, amount={}, quantity={}, gross={}, net={}, withholding={}", 
                             dividend.getExDate(), dividend.getExDate(), dividend.getPaymentDate(), 
                             dividendPerShare, totalQuantity, grossDividendAmount, netDividendAmount,
                             dividend.getWithholdingTaxRate());
                    
                    return netDividendAmount;
                })
                .sum();
        
        log.debug("Total lot-based dividends for contract {}: {}", contract.getContractId(), totalDividends);
        return totalDividends;
    }
    
    /**
     * Calculate contract-based dividends with detailed withholding tax information
     */
    private double calculateContractBasedDividendsWithDetails(CashFlowRequest.Contract contract,
                                                              List<Dividend> dividends,
                                                              List<WithholdingTaxInfo> withholdingTaxDetails) {
        
        double notional = contract.getNotionalAmount() != null ? contract.getNotionalAmount() : 1000000.0;
        
        return dividends.stream()
            .filter(dividend -> isDividendInPeriod(dividend, contract.getStartDate(), contract.getEndDate()))
            .mapToDouble(dividend -> {
                double grossDividendAmount = notional * dividend.getAmount() / 1000000.0; // Scale by notional
                double netDividendAmount = applyWithholdingTax(dividend, grossDividendAmount);
                
                // Create withholding tax info
                WithholdingTaxInfo withholdingInfo = createWithholdingTaxInfo(
                        contract, dividend, grossDividendAmount, netDividendAmount, 
                        LocalDate.now(), "CONTRACT_BASED", null);
                withholdingTaxDetails.add(withholdingInfo);
                
                return netDividendAmount;
            })
            .sum();
    }
    
    /**
     * Create WithholdingTaxInfo from dividend and calculation details
     */
    private WithholdingTaxInfo createWithholdingTaxInfo(CashFlowRequest.Contract contract,
                                                        Dividend dividend,
                                                        double grossDividendAmount,
                                                        double netDividendAmount,
                                                        LocalDate calculationDate,
                                                        String calculationType,
                                                        String lotId) {
        
        double withholdingTaxRate = dividend.getWithholdingTaxRate() != null ? dividend.getWithholdingTaxRate() : 0.0;
        double withholdingTaxAmount = grossDividendAmount - netDividendAmount;
        
        return WithholdingTaxInfo.builder()
                .contractId(contract.getContractId())
                .lotId(lotId)
                .underlying(contract.getUnderlying())
                .currency(contract.getCurrency())
                .exDate(dividend.getExDate())
                .paymentDate(dividend.getPaymentDate())
                .grossDividendAmount(grossDividendAmount)
                .withholdingTaxRate(withholdingTaxRate)
                .withholdingTaxAmount(withholdingTaxAmount)
                .netDividendAmount(netDividendAmount)
                .withholdingTreatment(mapWithholdingTreatment(dividend.getWithholdingTreatment()))
                .taxJurisdiction(determineTaxJurisdiction(contract.getCurrency()))
                .taxUtilityReference(generateTaxUtilityReference(contract.getContractId(), dividend.getExDate()))
                .calculationDate(calculationDate)
                .calculationType(calculationType)
                .build();
    }
    
    /**
     * Determine tax jurisdiction based on currency
     */
    private String determineTaxJurisdiction(String currency) {
        switch (currency) {
            case "USD": return "US";
            case "EUR": return "EU";
            case "GBP": return "UK";
            case "JPY": return "JP";
            case "CAD": return "CA";
            case "AUD": return "AU";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Generate tax utility reference
     */
    private String generateTaxUtilityReference(String contractId, LocalDate exDate) {
        return String.format("TAX_%s_%s", contractId, exDate.toString().replace("-", ""));
    }
    
    /**
     * Map Dividend.WithholdingTreatment to WithholdingTaxInfo.WithholdingTreatment
     */
    private WithholdingTaxInfo.WithholdingTreatment mapWithholdingTreatment(Dividend.WithholdingTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        return WithholdingTaxInfo.WithholdingTreatment.valueOf(treatment.name());
    }
}
