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
import java.util.UUID;

/**
 * Implementation of CashFlowEngine with core calculation logic.
 */
@Slf4j
@Service
public class CashFlowEngineImpl implements CashFlowEngine {

    @Override
    public List<CashFlowResponse.CashFlow> calculateCashFlows(CashFlowRequestContent request) {
        log.info("Starting cash flow calculation for contract: {}", request.getContractId());
        
        List<CashFlowResponse.CashFlow> cashFlows = new ArrayList<>();
        
        // Calculate interest cash flows
        cashFlows.addAll(calculateInterestCashFlows(request));
        
        // Calculate dividend cash flows
        cashFlows.addAll(calculateDividendCashFlows(request));
        
        // Calculate P&L cash flows
        cashFlows.addAll(calculatePnLCashFlows(request));
        
        // Calculate principal cash flows
        cashFlows.addAll(calculatePrincipalCashFlows(request));
        
        log.info("Completed cash flow calculation for contract: {}. Generated {} cash flows", 
            request.getContractId(), cashFlows.size());
        
        return cashFlows;
    }

    private List<CashFlowResponse.CashFlow> calculateInterestCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> interestFlows = new ArrayList<>();
        
        for (CashFlowRequestContent.Contract contract : request.getContracts()) {
            if (contract.getInterestLeg() != null && contract.getInterestRate() != null) {
                List<CashFlowResponse.CashFlow> contractInterestFlows = calculateContractInterestFlows(contract, request);
                interestFlows.addAll(contractInterestFlows);
            }
        }
        
        return interestFlows;
    }

    private List<CashFlowResponse.CashFlow> calculateContractInterestFlows(
            CashFlowRequestContent.Contract contract, 
            CashFlowRequestContent request) {
        
        List<CashFlowResponse.CashFlow> flows = new ArrayList<>();
        LocalDate fromDate = request.getDateRange().getFromDate();
        LocalDate toDate = request.getDateRange().getToDate();
        
        // Calculate daily interest accrual
        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            if (isBusinessDay(currentDate) && isWithinContractPeriod(currentDate, contract)) {
                double dailyInterest = calculateDailyInterest(contract, currentDate);
                
                if (dailyInterest != 0) {
                    CashFlowResponse.CashFlow interestFlow = CashFlowResponse.CashFlow.builder()
                        .cashFlowId("INT_" + UUID.randomUUID().toString())
                        .contractId(contract.getContractId())
                        .lotId("LOT_" + contract.getContractId()) // Default lot ID
                        .cashFlowType("INTEREST")
                        .cashFlowDate(currentDate)
                        .amount(dailyInterest)
                        .currency(contract.getCurrency())
                        .status("ACCRUAL")
                        .calculationBasis("DAILY_CLOSE")
                        .accrualStartDate(currentDate)
                        .accrualEndDate(currentDate)
                        .build();
                    
                    flows.add(interestFlow);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return flows;
    }

    private List<CashFlowResponse.CashFlow> calculateDividendCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> dividendFlows = new ArrayList<>();
        
        // Check if there are any dividend payment schedules
        if (request.getPaymentSchedules() != null) {
            for (CashFlowRequestContent.PaymentSchedule schedule : request.getPaymentSchedules()) {
                if ("DIVIDEND".equals(schedule.getPaymentType())) {
                    if (isDateInRange(schedule.getPaymentDate(), request.getDateRange())) {
                        CashFlowResponse.CashFlow dividendFlow = CashFlowResponse.CashFlow.builder()
                            .cashFlowId("DIV_" + UUID.randomUUID().toString())
                            .contractId(schedule.getContractId())
                            .lotId("LOT_" + schedule.getContractId())
                            .cashFlowType("DIVIDEND")
                            .cashFlowDate(schedule.getPaymentDate())
                            .amount(schedule.getPaymentAmount())
                            .currency(schedule.getCurrency())
                            .status("REALIZED_UNSETTLED")
                            .calculationBasis("SCHEDULED")
                            .settlementDate(schedule.getPaymentDate())
                            .build();
                        
                        dividendFlows.add(dividendFlow);
                    }
                }
            }
        }
        
        return dividendFlows;
    }

    private List<CashFlowResponse.CashFlow> calculatePnLCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> pnlFlows = new ArrayList<>();
        
        // Calculate P&L for each lot
        for (CashFlowRequestContent.Lot lot : request.getLots()) {
            double pnl = calculateLotPnL(lot, request);
            
            if (pnl != 0) {
                CashFlowResponse.CashFlow pnlFlow = CashFlowResponse.CashFlow.builder()
                    .cashFlowId("PNL_" + UUID.randomUUID().toString())
                    .contractId(lot.getPositionId()) // Using position ID as contract reference
                    .lotId(lot.getLotId())
                    .cashFlowType("PNL")
                    .cashFlowDate(lot.getTradeDate())
                    .amount(pnl)
                    .currency(lot.getCurrency())
                    .status("REALIZED_UNSETTLED")
                    .calculationBasis("TRADE_LEVEL")
                    .settlementDate(lot.getSettlementDate())
                    .build();
                
                pnlFlows.add(pnlFlow);
            }
        }
        
        return pnlFlows;
    }

    private List<CashFlowResponse.CashFlow> calculatePrincipalCashFlows(CashFlowRequestContent request) {
        List<CashFlowResponse.CashFlow> principalFlows = new ArrayList<>();
        
        // Check for principal payment schedules
        if (request.getPaymentSchedules() != null) {
            for (CashFlowRequestContent.PaymentSchedule schedule : request.getPaymentSchedules()) {
                if ("PRINCIPAL".equals(schedule.getPaymentType())) {
                    if (isDateInRange(schedule.getPaymentDate(), request.getDateRange())) {
                        CashFlowResponse.CashFlow principalFlow = CashFlowResponse.CashFlow.builder()
                            .cashFlowId("PRIN_" + UUID.randomUUID().toString())
                            .contractId(schedule.getContractId())
                            .lotId("LOT_" + schedule.getContractId())
                            .cashFlowType("PRINCIPAL")
                            .cashFlowDate(schedule.getPaymentDate())
                            .amount(schedule.getPaymentAmount())
                            .currency(schedule.getCurrency())
                            .status("REALIZED_UNSETTLED")
                            .calculationBasis("SCHEDULED")
                            .settlementDate(schedule.getPaymentDate())
                            .build();
                        
                        principalFlows.add(principalFlow);
                    }
                }
            }
        }
        
        return principalFlows;
    }

    private double calculateDailyInterest(CashFlowRequestContent.Contract contract, LocalDate date) {
        // Get the notional amount for this date
        double notionalAmount = getNotionalAmountForDate(contract, date);
        
        // Get the interest rate for this date
        double interestRate = getInterestRateForDate(contract, date);
        
        // Calculate daily interest using ACT/360 day count convention
        double dailyInterest = notionalAmount * interestRate / 360.0;
        
        return Math.round(dailyInterest * 100.0) / 100.0; // Round to 2 decimal places
    }

    private double getNotionalAmountForDate(CashFlowRequestContent.Contract contract, LocalDate date) {
        // For simplicity, use the contract notional amount
        // In a real implementation, this would consider position changes over time
        return contract.getNotionalAmount();
    }

    private double getInterestRateForDate(CashFlowRequestContent.Contract contract, LocalDate date) {
        // For simplicity, use the contract interest rate
        // In a real implementation, this would fetch from market data or rate curves
        return contract.getInterestRate() != null ? contract.getInterestRate() : 0.0;
    }

    private double calculateLotPnL(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        // Calculate P&L based on current market price vs trade price
        double currentPrice = getCurrentMarketPrice(lot, request);
        double tradePrice = lot.getPrice();
        double quantity = lot.getQuantity();
        
        double pnl = (currentPrice - tradePrice) * quantity;
        
        return Math.round(pnl * 100.0) / 100.0; // Round to 2 decimal places
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

    private boolean isBusinessDay(LocalDate date) {
        // Simple business day check - exclude weekends
        // In a real implementation, this would use a holiday calendar
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 5; // Monday to Friday
    }

    private boolean isWithinContractPeriod(LocalDate date, CashFlowRequestContent.Contract contract) {
        return !date.isBefore(contract.getStartDate()) && !date.isAfter(contract.getEndDate());
    }

    private boolean isDateInRange(LocalDate date, CashFlowRequestContent.DateRange range) {
        return !date.isBefore(range.getFromDate()) && !date.isAfter(range.getToDate());
    }
}
