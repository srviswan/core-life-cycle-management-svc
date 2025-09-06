package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.InterestCalculationEngine;
import com.financial.cashflow.service.MarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of InterestCalculationEngine with comprehensive interest calculation logic.
 * Supports multiple day count conventions, rate resets, and business day adjustments.
 */
@Slf4j
@Service
public class InterestCalculationEngineImpl implements InterestCalculationEngine {

    @Autowired
    private MarketDataService marketDataService;

    // Cache for business day calculations
    private final Map<LocalDate, Boolean> businessDayCache = new ConcurrentHashMap<>();
    
    // Cache for interest rate calculations
    private final Map<String, Double> interestRateCache = new ConcurrentHashMap<>();

    @Override
    public List<CashFlowResponse.CashFlow> calculateInterestFlows(
            CashFlowRequestContent.Contract contract,
            CashFlowRequestContent.DateRange dateRange,
            CashFlowRequestContent request) {
        
        log.debug("Calculating interest flows for contract: {} from {} to {}", 
            contract.getContractId(), dateRange.getFromDate(), dateRange.getToDate());
        
        List<CashFlowResponse.CashFlow> interestFlows = new ArrayList<>();
        
        // Get interest leg configuration
        CashFlowRequestContent.InterestLeg interestLeg = contract.getInterestLeg();
        String dayCountConvention = interestLeg != null ? interestLeg.getDayCountConvention() : "ACT/360";
        String paymentFrequency = interestLeg != null ? interestLeg.getPaymentFrequency() : "MONTHLY";
        String resetFrequency = interestLeg != null ? interestLeg.getResetFrequency() : "MONTHLY";
        
        // Calculate interest periods based on payment frequency
        List<InterestPeriod> interestPeriods = calculateInterestPeriods(
            dateRange.getFromDate(), dateRange.getToDate(), paymentFrequency, resetFrequency);
        
        for (InterestPeriod period : interestPeriods) {
            if (isWithinContractPeriod(period.getStartDate(), contract) && 
                isWithinContractPeriod(period.getEndDate(), contract)) {
                
                double periodInterest = calculatePeriodInterest(contract, period.getStartDate(), 
                    period.getEndDate(), dayCountConvention, request);
                
                if (periodInterest != 0) {
                    // Create accrual cash flow
                    CashFlowResponse.CashFlow accrualFlow = createInterestCashFlow(
                        contract, period, periodInterest, "ACCRUAL", request);
                    interestFlows.add(accrualFlow);
                    
                    // Create payment cash flow if it's a payment date
                    if (isPaymentDate(period.getEndDate(), paymentFrequency)) {
                        CashFlowResponse.CashFlow paymentFlow = createInterestCashFlow(
                            contract, period, periodInterest, "REALIZED_UNSETTLED", request);
                        paymentFlow.setSettlementDate(calculateSettlementDate(period.getEndDate()));
                        interestFlows.add(paymentFlow);
                    }
                }
            }
        }
        
        log.debug("Generated {} interest flows for contract: {}", interestFlows.size(), contract.getContractId());
        return interestFlows;
    }

    @Override
    public double calculatePeriodInterest(
            CashFlowRequestContent.Contract contract,
            LocalDate startDate,
            LocalDate endDate,
            String dayCountConvention,
            CashFlowRequestContent request) {
        
        double notionalAmount = getNotionalAmountForPeriod(contract, startDate, endDate, request);
        double interestRate = getEffectiveRate(contract, startDate, request);
        int days = calculateDays(startDate, endDate, dayCountConvention);
        
        double periodInterest = notionalAmount * interestRate * days / getDayCountDenominator(dayCountConvention);
        
        return Math.round(periodInterest * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public double getEffectiveRate(
            CashFlowRequestContent.Contract contract,
            LocalDate date,
            CashFlowRequestContent request) {
        
        String cacheKey = contract.getContractId() + "_" + date.toString();
        
        return interestRateCache.computeIfAbsent(cacheKey, k -> {
            // Try to get rate from market data first
            if (contract.getInterestRateIndex() != null) {
                String tenor = getTenorFromContract(contract);
                return marketDataService.getRateWithFallback(
                    contract.getInterestRateIndex(), date, tenor, 
                    contract.getInterestRate() != null ? contract.getInterestRate() : 0.0, request);
            }
            
            // Fallback to contract rate
            return contract.getInterestRate() != null ? contract.getInterestRate() : 0.0;
        });
    }

    @Override
    public int calculateDays(LocalDate startDate, LocalDate endDate, String dayCountConvention) {
        switch (dayCountConvention) {
            case "ACT/360":
            case "ACT/365":
                return (int) ChronoUnit.DAYS.between(startDate, endDate);
            case "30/360":
                return calculate30_360Days(startDate, endDate);
            case "ACT/ACT":
                return calculateACT_ACTDays(startDate, endDate);
            default:
                return (int) ChronoUnit.DAYS.between(startDate, endDate);
        }
    }

    @Override
    public int getDayCountDenominator(String dayCountConvention) {
        switch (dayCountConvention) {
            case "ACT/360":
                return 360;
            case "ACT/365":
                return 365;
            case "30/360":
                return 360;
            case "ACT/ACT":
                return 365; // Simplified for leap year handling
            default:
                return 360;
        }
    }

    @Override
    public boolean isBusinessDay(LocalDate date) {
        return businessDayCache.computeIfAbsent(date, d -> {
            int dayOfWeek = d.getDayOfWeek().getValue();
            return dayOfWeek >= 1 && dayOfWeek <= 5; // Monday to Friday
        });
    }

    @Override
    public List<InterestPeriod> calculateInterestPeriods(
            LocalDate fromDate,
            LocalDate toDate,
            String paymentFrequency,
            String resetFrequency) {
        
        List<InterestPeriod> periods = new ArrayList<>();
        LocalDate currentDate = fromDate;
        
        while (!currentDate.isAfter(toDate)) {
            LocalDate periodEnd = calculatePeriodEnd(currentDate, paymentFrequency);
            if (periodEnd.isAfter(toDate)) {
                periodEnd = toDate;
            }
            
            String resetType = isResetDate(currentDate, resetFrequency) ? "RESET" : "REGULAR";
            periods.add(new InterestPeriod(currentDate, periodEnd, resetType));
            currentDate = periodEnd.plusDays(1);
        }
        
        return periods;
    }

    // Helper methods

    private double getNotionalAmountForPeriod(CashFlowRequestContent.Contract contract, 
                                            LocalDate startDate, LocalDate endDate, 
                                            CashFlowRequestContent request) {
        // In a real implementation, this would consider position changes over time
        // For now, use the contract notional amount
        return contract.getNotionalAmount();
    }

    private String getTenorFromContract(CashFlowRequestContent.Contract contract) {
        // Extract tenor from interest leg or use default
        if (contract.getInterestLeg() != null) {
            // Could extract from payment frequency or other fields
            return "3M"; // Default 3-month tenor
        }
        return "3M";
    }

    private int calculate30_360Days(LocalDate startDate, LocalDate endDate) {
        int startDay = Math.min(startDate.getDayOfMonth(), 30);
        int endDay = Math.min(endDate.getDayOfMonth(), 30);
        
        int days = (endDate.getYear() - startDate.getYear()) * 360 +
                  (endDate.getMonthValue() - startDate.getMonthValue()) * 30 +
                  (endDay - startDay);
        
        return days;
    }

    private int calculateACT_ACTDays(LocalDate startDate, LocalDate endDate) {
        // Simplified ACT/ACT calculation
        // In a real implementation, this would handle leap years properly
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }

    private LocalDate calculatePeriodEnd(LocalDate startDate, String frequency) {
        switch (frequency) {
            case "DAILY":
                return startDate;
            case "WEEKLY":
                return startDate.plusWeeks(1).minusDays(1);
            case "MONTHLY":
                return startDate.plusMonths(1).minusDays(1);
            case "QUARTERLY":
                return startDate.plusMonths(3).minusDays(1);
            case "YEARLY":
                return startDate.plusYears(1).minusDays(1);
            default:
                return startDate.plusMonths(1).minusDays(1);
        }
    }

    private boolean isPaymentDate(LocalDate date, String frequency) {
        // Check if this is a payment date based on frequency
        switch (frequency) {
            case "DAILY":
                return true;
            case "WEEKLY":
                return date.getDayOfWeek().getValue() == 5; // Friday
            case "MONTHLY":
                return date.equals(date.withDayOfMonth(date.lengthOfMonth()));
            case "QUARTERLY":
                return date.getMonthValue() % 3 == 0 && 
                       date.equals(date.withDayOfMonth(date.lengthOfMonth()));
            case "YEARLY":
                return date.getMonthValue() == 12 && 
                       date.equals(date.withDayOfMonth(date.lengthOfMonth()));
            default:
                return date.equals(date.withDayOfMonth(date.lengthOfMonth()));
        }
    }

    private boolean isResetDate(LocalDate date, String resetFrequency) {
        // Check if this is a rate reset date
        return isPaymentDate(date, resetFrequency);
    }

    private LocalDate calculateSettlementDate(LocalDate paymentDate) {
        // T+2 settlement for most interest payments
        return paymentDate.plusDays(2);
    }

    private boolean isWithinContractPeriod(LocalDate date, CashFlowRequestContent.Contract contract) {
        return !date.isBefore(contract.getStartDate()) && !date.isAfter(contract.getEndDate());
    }

    private CashFlowResponse.CashFlow createInterestCashFlow(
            CashFlowRequestContent.Contract contract,
            InterestPeriod period,
            double amount,
            String status,
            CashFlowRequestContent request) {
        
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("INT_" + UUID.randomUUID().toString())
            .contractId(contract.getContractId())
            .lotId("LOT_" + contract.getContractId())
            .cashFlowType("INTEREST")
            .cashFlowDate(period.getEndDate())
            .amount(amount)
            .currency(contract.getCurrency())
            .status(status)
            .calculationBasis("PERIOD_BASED")
            .accrualStartDate(period.getStartDate())
            .accrualEndDate(period.getEndDate())
            .build();
    }
}
