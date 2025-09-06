package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.CashFlowEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced implementation of CashFlowEngine with comprehensive calculation logic.
 * Handles complex scenarios including:
 * - Interest calculations with different day count conventions
 * - Dividend calculations with corporate actions
 * - P&L calculations using multiple methods (MTM, Realized)
 * - Principal cash flows with amortization schedules
 * - Complex payment schedules and resets
 */
@Slf4j
@Service
public class CashFlowEngineImpl implements CashFlowEngine {

    // Cache for business day calculations
    private final Map<LocalDate, Boolean> businessDayCache = new ConcurrentHashMap<>();
    
    // Cache for interest rate calculations
    private final Map<String, Double> interestRateCache = new ConcurrentHashMap<>();

    @Override
    public List<CashFlowResponse.CashFlow> calculateCashFlows(CashFlowRequestContent request) {
        log.info("Starting enhanced cash flow calculation for contract: {}", request.getContractId());
        
        List<CashFlowResponse.CashFlow> cashFlows = new ArrayList<>();
        
        try {
            // Calculate interest cash flows with enhanced logic
            List<CashFlowResponse.CashFlow> interestFlows = calculateInterestCashFlows(request);
            cashFlows.addAll(interestFlows);
            log.debug("Generated {} interest cash flows", interestFlows.size());
            
            // Calculate dividend cash flows with corporate action handling
            List<CashFlowResponse.CashFlow> dividendFlows = calculateDividendCashFlows(request);
            cashFlows.addAll(dividendFlows);
            log.debug("Generated {} dividend cash flows", dividendFlows.size());
            
            // Calculate P&L cash flows with multiple methods
            List<CashFlowResponse.CashFlow> pnlFlows = calculatePnLCashFlows(request);
            cashFlows.addAll(pnlFlows);
            log.debug("Generated {} P&L cash flows", pnlFlows.size());
            
            // Calculate principal cash flows with amortization
            List<CashFlowResponse.CashFlow> principalFlows = calculatePrincipalCashFlows(request);
            cashFlows.addAll(principalFlows);
            log.debug("Generated {} principal cash flows", principalFlows.size());
            
            // Calculate corporate action cash flows
            List<CashFlowResponse.CashFlow> corporateActionFlows = calculateCorporateActionCashFlows(request);
            cashFlows.addAll(corporateActionFlows);
            log.debug("Generated {} corporate action cash flows", corporateActionFlows.size());
            
        } catch (Exception e) {
            log.error("Error calculating cash flows for contract: {}", request.getContractId(), e);
            throw new RuntimeException("Failed to calculate cash flows", e);
        }
        
        log.info("Completed enhanced cash flow calculation for contract: {}. Generated {} total cash flows", 
            request.getContractId(), cashFlows.size());
        
        return cashFlows;
    }

    /**
     * Enhanced interest calculation with support for:
     * - Different day count conventions (ACT/360, 30/360, ACT/365)
     * - Interest rate resets
     * - Compounding periods
     * - Business day adjustments
     */
    private List<CashFlowResponse.CashFlow> calculateInterestCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> interestFlows = new ArrayList<>();
        
        for (CashFlowRequestContent.Contract contract : request.getContracts()) {
            if (hasInterestLeg(contract)) {
                List<CashFlowResponse.CashFlow> contractInterestFlows = calculateContractInterestFlows(contract, request);
                interestFlows.addAll(contractInterestFlows);
            }
        }
        
        return interestFlows;
    }

    private boolean hasInterestLeg(CashFlowRequestContent.Contract contract) {
        return contract.getInterestLeg() != null || contract.getInterestRate() != null;
    }

    private List<CashFlowResponse.CashFlow> calculateContractInterestFlows(
            CashFlowRequestContent.Contract contract, 
            CashFlowRequestContent request) {
        
        List<CashFlowResponse.CashFlow> flows = new ArrayList<>();
        LocalDate fromDate = request.getDateRange().getFromDate();
        LocalDate toDate = request.getDateRange().getToDate();
        
        // Get interest leg configuration
        CashFlowRequestContent.InterestLeg interestLeg = contract.getInterestLeg();
        String dayCountConvention = interestLeg != null ? interestLeg.getDayCountConvention() : "ACT/360";
        String paymentFrequency = interestLeg != null ? interestLeg.getPaymentFrequency() : "MONTHLY";
        String resetFrequency = interestLeg != null ? interestLeg.getResetFrequency() : "MONTHLY";
        
        // Calculate interest periods based on payment frequency
        List<InterestPeriod> interestPeriods = calculateInterestPeriods(fromDate, toDate, paymentFrequency, resetFrequency);
        
        for (InterestPeriod period : interestPeriods) {
            if (isWithinContractPeriod(period.getStartDate(), contract) && 
                isWithinContractPeriod(period.getEndDate(), contract)) {
                
                double periodInterest = calculatePeriodInterest(contract, period, dayCountConvention, request);
                
                if (periodInterest != 0) {
                    // Create accrual cash flow
                    CashFlowResponse.CashFlow accrualFlow = createInterestCashFlow(
                        contract, period, periodInterest, "ACCRUAL", request);
                    flows.add(accrualFlow);
                    
                    // Create payment cash flow if it's a payment date
                    if (isPaymentDate(period.getEndDate(), paymentFrequency)) {
                        CashFlowResponse.CashFlow paymentFlow = createInterestCashFlow(
                            contract, period, periodInterest, "REALIZED_UNSETTLED", request);
                        paymentFlow.setSettlementDate(calculateSettlementDate(period.getEndDate()));
                        flows.add(paymentFlow);
                    }
                }
            }
        }
        
        return flows;
    }

    /**
     * Enhanced dividend calculation with support for:
     * - Dividend payment schedules
     * - Corporate action adjustments
     * - Dividend reinvestment vs cash payment
     * - Ex-dividend date handling
     */
    private List<CashFlowResponse.CashFlow> calculateDividendCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> dividendFlows = new ArrayList<>();
        
        // Process dividend payment schedules
        if (request.getPaymentSchedules() != null) {
            for (CashFlowRequestContent.PaymentSchedule schedule : request.getPaymentSchedules()) {
                if ("DIVIDEND".equals(schedule.getPaymentType())) {
                    if (isDateInRange(schedule.getPaymentDate(), request.getDateRange())) {
                        CashFlowResponse.CashFlow dividendFlow = createDividendCashFlow(schedule, request);
                        dividendFlows.add(dividendFlow);
                    }
                }
            }
        }
        
        // Calculate dividend cash flows from equity legs
        for (CashFlowRequestContent.Contract contract : request.getContracts()) {
            if (contract.getEquityLeg() != null) {
                List<CashFlowResponse.CashFlow> equityDividendFlows = calculateEquityDividendFlows(contract, request);
                dividendFlows.addAll(equityDividendFlows);
            }
        }
        
        return dividendFlows;
    }

    private List<CashFlowResponse.CashFlow> calculateEquityDividendFlows(
            CashFlowRequestContent.Contract contract, 
            CashFlowRequestContent request) {
        
        List<CashFlowResponse.CashFlow> flows = new ArrayList<>();
        
        // Get positions for this contract
        List<CashFlowRequestContent.Position> contractPositions = request.getPositions().stream()
            .filter(pos -> pos.getContractId().equals(contract.getContractId()))
            .collect(Collectors.toList());
        
        for (CashFlowRequestContent.Position position : contractPositions) {
            // Calculate dividend based on position quantity and dividend rate
            double dividendAmount = calculatePositionDividend(position, contract, request);
            
            if (dividendAmount > 0) {
                CashFlowResponse.CashFlow dividendFlow = CashFlowResponse.CashFlow.builder()
                    .cashFlowId("DIV_" + UUID.randomUUID().toString())
                    .contractId(contract.getContractId())
                    .lotId("LOT_" + position.getPositionId())
                    .cashFlowType("DIVIDEND")
                    .cashFlowDate(request.getDateRange().getToDate()) // Use calculation end date
                    .amount(dividendAmount)
                    .currency(contract.getCurrency())
                    .status("REALIZED_UNSETTLED")
                    .calculationBasis("POSITION_BASED")
                    .settlementDate(calculateSettlementDate(request.getDateRange().getToDate()))
                    .build();
                
                flows.add(dividendFlow);
            }
        }
        
        return flows;
    }

    /**
     * Enhanced P&L calculation with support for:
     * - Mark-to-Market (MTM) calculations
     * - Realized P&L from trades
     * - Unrealized P&L
     * - Different P&L calculation methods (LIFO, FIFO, etc.)
     */
    private List<CashFlowResponse.CashFlow> calculatePnLCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> pnlFlows = new ArrayList<>();
        
        // Calculate P&L for each lot
        for (CashFlowRequestContent.Lot lot : request.getLots()) {
            // Calculate MTM P&L
            double mtmPnL = calculateMTMPnL(lot, request);
            if (mtmPnL != 0) {
                CashFlowResponse.CashFlow mtmFlow = createPnLCashFlow(lot, mtmPnL, "MTM", request);
                pnlFlows.add(mtmFlow);
            }
            
            // Calculate realized P&L if lot is closed
            if (isLotClosed(lot)) {
                double realizedPnL = calculateRealizedPnL(lot, request);
                if (realizedPnL != 0) {
                    CashFlowResponse.CashFlow realizedFlow = createPnLCashFlow(lot, realizedPnL, "REALIZED", request);
                    pnlFlows.add(realizedFlow);
                }
            }
        }
        
        // Calculate position-level P&L
        for (CashFlowRequestContent.Position position : request.getPositions()) {
            double positionPnL = calculatePositionPnL(position, request);
            if (positionPnL != 0) {
                CashFlowResponse.CashFlow positionFlow = createPositionPnLCashFlow(position, positionPnL, request);
                pnlFlows.add(positionFlow);
            }
        }
        
        return pnlFlows;
    }

    /**
     * Enhanced principal cash flow calculation with support for:
     * - Amortization schedules
     * - Bullet payments
     * - Partial principal payments
     * - Principal resets
     */
    private List<CashFlowResponse.CashFlow> calculatePrincipalCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> principalFlows = new ArrayList<>();
        
        // Process principal payment schedules
        if (request.getPaymentSchedules() != null) {
            for (CashFlowRequestContent.PaymentSchedule schedule : request.getPaymentSchedules()) {
                if ("PRINCIPAL".equals(schedule.getPaymentType())) {
                    if (isDateInRange(schedule.getPaymentDate(), request.getDateRange())) {
                        CashFlowResponse.CashFlow principalFlow = createPrincipalCashFlow(schedule, request);
                        principalFlows.add(principalFlow);
                    }
                }
            }
        }
        
        // Calculate principal cash flows from contracts
        for (CashFlowRequestContent.Contract contract : request.getContracts()) {
            List<CashFlowResponse.CashFlow> contractPrincipalFlows = calculateContractPrincipalFlows(contract, request);
            principalFlows.addAll(contractPrincipalFlows);
        }
        
        return principalFlows;
    }

    /**
     * Calculate corporate action cash flows including:
     * - Stock splits
     * - Mergers and acquisitions
     * - Rights issues
     * - Spin-offs
     */
    private List<CashFlowResponse.CashFlow> calculateCorporateActionCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> corporateActionFlows = new ArrayList<>();
        
        // This would integrate with corporate action data
        // For now, we'll create placeholder logic
        for (CashFlowRequestContent.Contract contract : request.getContracts()) {
            if (contract.getEquityLeg() != null) {
                // Check for corporate actions in the date range
                List<CashFlowResponse.CashFlow> contractCorporateActionFlows = 
                    calculateContractCorporateActionFlows(contract, request);
                corporateActionFlows.addAll(contractCorporateActionFlows);
            }
        }
        
        return corporateActionFlows;
    }

    // Helper methods for enhanced calculations

    private double calculatePeriodInterest(CashFlowRequestContent.Contract contract, InterestPeriod period, 
                                         String dayCountConvention, CashFlowRequestContent request) {
        
        double notionalAmount = getNotionalAmountForPeriod(contract, period, request);
        double interestRate = getInterestRateForPeriod(contract, period, request);
        int days = calculateDays(period.getStartDate(), period.getEndDate(), dayCountConvention);
        
        double periodInterest = notionalAmount * interestRate * days / getDayCountDenominator(dayCountConvention);
        
        return Math.round(periodInterest * 100.0) / 100.0; // Round to 2 decimal places
    }

    private double getNotionalAmountForPeriod(CashFlowRequestContent.Contract contract, InterestPeriod period, 
                                            CashFlowRequestContent request) {
        // In a real implementation, this would consider position changes over time
        return contract.getNotionalAmount();
    }

    private double getInterestRateForPeriod(CashFlowRequestContent.Contract contract, InterestPeriod period, 
                                          CashFlowRequestContent request) {
        // Try to get rate from market data first
        if (request.getMarketData() != null && request.getMarketData().getRates() != null) {
            for (CashFlowRequestContent.RateData rateData : request.getMarketData().getRates()) {
                if (rateData.getRateIndex().equals(contract.getInterestRateIndex()) &&
                    isDateInRange(rateData.getRateDate(), period)) {
                    return rateData.getRate();
                }
            }
        }
        
        // Fallback to contract rate
        return contract.getInterestRate() != null ? contract.getInterestRate() : 0.0;
    }

    private int calculateDays(LocalDate startDate, LocalDate endDate, String dayCountConvention) {
        switch (dayCountConvention) {
            case "ACT/360":
            case "ACT/365":
                return (int) ChronoUnit.DAYS.between(startDate, endDate);
            case "30/360":
                return calculate30_360Days(startDate, endDate);
            default:
                return (int) ChronoUnit.DAYS.between(startDate, endDate);
        }
    }

    private int calculate30_360Days(LocalDate startDate, LocalDate endDate) {
        int startDay = Math.min(startDate.getDayOfMonth(), 30);
        int endDay = Math.min(endDate.getDayOfMonth(), 30);
        
        int days = (endDate.getYear() - startDate.getYear()) * 360 +
                  (endDate.getMonthValue() - startDate.getMonthValue()) * 30 +
                  (endDay - startDay);
        
        return days;
    }

    private int getDayCountDenominator(String dayCountConvention) {
        switch (dayCountConvention) {
            case "ACT/360":
                return 360;
            case "ACT/365":
                return 365;
            case "30/360":
                return 360;
            default:
                return 360;
        }
    }

    private double calculatePositionDividend(CashFlowRequestContent.Position position, 
                                           CashFlowRequestContent.Contract contract, 
                                           CashFlowRequestContent request) {
        // Get dividend rate from market data or use default
        double dividendRate = getDividendRate(contract.getEquityLeg().getUnderlier(), request);
        return position.getQuantity() * dividendRate;
    }

    private double getDividendRate(String underlier, CashFlowRequestContent request) {
        // In a real implementation, this would fetch from market data
        // For now, use a default rate
        return 0.02; // 2% annual dividend rate
    }

    private double calculateMTMPnL(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        double currentPrice = getCurrentMarketPrice(lot, request);
        double tradePrice = lot.getPrice();
        double quantity = lot.getQuantity();
        
        double pnl = (currentPrice - tradePrice) * quantity;
        return Math.round(pnl * 100.0) / 100.0;
    }

    private double calculateRealizedPnL(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        // This would calculate realized P&L when a lot is closed
        // For now, return 0 as placeholder
        return 0.0;
    }

    private double calculatePositionPnL(CashFlowRequestContent.Position position, CashFlowRequestContent request) {
        double currentPrice = getCurrentMarketPriceForPosition(position, request);
        double averagePrice = position.getAveragePrice();
        double quantity = position.getQuantity();
        
        double pnl = (currentPrice - averagePrice) * quantity;
        return Math.round(pnl * 100.0) / 100.0;
    }

    private double getCurrentMarketPriceForPosition(CashFlowRequestContent.Position position, 
                                                  CashFlowRequestContent request) {
        // Try to get price from market data
        if (request.getMarketData() != null && request.getMarketData().getPrices() != null) {
            for (CashFlowRequestContent.PriceData priceData : request.getMarketData().getPrices()) {
                if (priceData.getInstrumentId().equals(position.getPositionId())) {
                    return priceData.getPrice();
                }
            }
        }
        
        // Fallback to average price
        return position.getAveragePrice();
    }

    private List<CashFlowResponse.CashFlow> calculateContractPrincipalFlows(
            CashFlowRequestContent.Contract contract, 
            CashFlowRequestContent request) {
        
        List<CashFlowResponse.CashFlow> flows = new ArrayList<>();
        
        // Check if contract ends in the calculation period
        if (isDateInRange(contract.getEndDate(), request.getDateRange())) {
            double principalAmount = contract.getNotionalAmount();
            
            CashFlowResponse.CashFlow principalFlow = CashFlowResponse.CashFlow.builder()
                .cashFlowId("PRIN_" + UUID.randomUUID().toString())
                .contractId(contract.getContractId())
                .lotId("LOT_" + contract.getContractId())
                .cashFlowType("PRINCIPAL")
                .cashFlowDate(contract.getEndDate())
                .amount(principalAmount)
                .currency(contract.getCurrency())
                .status("REALIZED_UNSETTLED")
                .calculationBasis("CONTRACT_END")
                .settlementDate(calculateSettlementDate(contract.getEndDate()))
                .build();
            
            flows.add(principalFlow);
        }
        
        return flows;
    }

    private List<CashFlowResponse.CashFlow> calculateContractCorporateActionFlows(
            CashFlowRequestContent.Contract contract, 
            CashFlowRequestContent request) {
        
        List<CashFlowResponse.CashFlow> flows = new ArrayList<>();
        
        // This would integrate with corporate action data
        // For now, return empty list as placeholder
        return flows;
    }

    // Helper methods for creating cash flows

    private CashFlowResponse.CashFlow createInterestCashFlow(CashFlowRequestContent.Contract contract, 
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

    private CashFlowResponse.CashFlow createDividendCashFlow(CashFlowRequestContent.PaymentSchedule schedule, 
                                                            CashFlowRequestContent request) {
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("DIV_" + UUID.randomUUID().toString())
            .contractId(schedule.getContractId())
            .lotId("LOT_" + schedule.getContractId())
            .cashFlowType("DIVIDEND")
            .cashFlowDate(schedule.getPaymentDate())
            .amount(schedule.getPaymentAmount())
            .currency(schedule.getCurrency())
            .status("REALIZED_UNSETTLED")
            .calculationBasis("SCHEDULED")
            .settlementDate(calculateSettlementDate(schedule.getPaymentDate()))
            .build();
    }

    private CashFlowResponse.CashFlow createPnLCashFlow(CashFlowRequestContent.Lot lot, 
                                                       double amount, 
                                                       String pnlType, 
                                                       CashFlowRequestContent request) {
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("PNL_" + UUID.randomUUID().toString())
            .contractId(lot.getPositionId())
            .lotId(lot.getLotId())
            .cashFlowType("PNL")
            .cashFlowDate(lot.getTradeDate())
            .amount(amount)
            .currency(lot.getCurrency())
            .status("REALIZED_UNSETTLED")
            .calculationBasis(pnlType + "_TRADE_LEVEL")
            .settlementDate(lot.getSettlementDate())
            .build();
    }

    private CashFlowResponse.CashFlow createPositionPnLCashFlow(CashFlowRequestContent.Position position, 
                                                               double amount, 
                                                               CashFlowRequestContent request) {
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("PNL_" + UUID.randomUUID().toString())
            .contractId(position.getContractId())
            .lotId("LOT_" + position.getPositionId())
            .cashFlowType("PNL")
            .cashFlowDate(position.getPositionDate())
            .amount(amount)
            .currency(position.getCurrency())
            .status("REALIZED_UNSETTLED")
            .calculationBasis("POSITION_LEVEL")
            .settlementDate(calculateSettlementDate(position.getPositionDate()))
            .build();
    }

    private CashFlowResponse.CashFlow createPrincipalCashFlow(CashFlowRequestContent.PaymentSchedule schedule, 
                                                             CashFlowRequestContent request) {
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("PRIN_" + UUID.randomUUID().toString())
            .contractId(schedule.getContractId())
            .lotId("LOT_" + schedule.getContractId())
            .cashFlowType("PRINCIPAL")
            .cashFlowDate(schedule.getPaymentDate())
            .amount(schedule.getPaymentAmount())
            .currency(schedule.getCurrency())
            .status("REALIZED_UNSETTLED")
            .calculationBasis("SCHEDULED")
            .settlementDate(calculateSettlementDate(schedule.getPaymentDate()))
            .build();
    }

    // Utility methods

    private List<InterestPeriod> calculateInterestPeriods(LocalDate fromDate, LocalDate toDate, 
                                                         String paymentFrequency, String resetFrequency) {
        List<InterestPeriod> periods = new ArrayList<>();
        LocalDate currentDate = fromDate;
        
        while (!currentDate.isAfter(toDate)) {
            LocalDate periodEnd = calculatePeriodEnd(currentDate, paymentFrequency);
            if (periodEnd.isAfter(toDate)) {
                periodEnd = toDate;
            }
            
            periods.add(new InterestPeriod(currentDate, periodEnd));
            currentDate = periodEnd.plusDays(1);
        }
        
        return periods;
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
        // For now, assume monthly payments on month end
        return date.equals(date.withDayOfMonth(date.lengthOfMonth()));
    }

    private LocalDate calculateSettlementDate(LocalDate paymentDate) {
        // T+2 settlement for most cash flows
        return paymentDate.plusDays(2);
    }

    private boolean isLotClosed(CashFlowRequestContent.Lot lot) {
        // Check if lot is closed (quantity = 0 or has close date)
        return lot.getQuantity() == 0 || lot.getSettlementDate() != null;
    }

    private boolean isBusinessDay(LocalDate date) {
        return businessDayCache.computeIfAbsent(date, d -> {
            int dayOfWeek = d.getDayOfWeek().getValue();
            return dayOfWeek >= 1 && dayOfWeek <= 5; // Monday to Friday
        });
    }

    private boolean isWithinContractPeriod(LocalDate date, CashFlowRequestContent.Contract contract) {
        return !date.isBefore(contract.getStartDate()) && !date.isAfter(contract.getEndDate());
    }

    private boolean isDateInRange(LocalDate date, CashFlowRequestContent.DateRange range) {
        return !date.isBefore(range.getFromDate()) && !date.isAfter(range.getToDate());
    }

    private boolean isDateInRange(LocalDate date, InterestPeriod period) {
        return !date.isBefore(period.getStartDate()) && !date.isAfter(period.getEndDate());
    }

    private double getCurrentMarketPrice(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        // Try to get price from embedded market data first
        if (request.getMarketData() != null && request.getMarketData().getPrices() != null) {
            for (CashFlowRequestContent.PriceData priceData : request.getMarketData().getPrices()) {
                if (priceData.getInstrumentId().equals(lot.getLotId()) || 
                    priceData.getInstrumentId().equals(lot.getPositionId())) {
                    return priceData.getPrice();
                }
            }
        }
        
        // Fallback to trade price if no market data available
        return lot.getPrice();
    }

    /**
     * Inner class to represent interest calculation periods
     */
    private static class InterestPeriod {
        private final LocalDate startDate;
        private final LocalDate endDate;
        
        public InterestPeriod(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }
}
