package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.EquityCalculationEngine;
import com.financial.cashflow.service.MarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of EquityCalculationEngine with comprehensive equity calculation logic.
 * Uses virtual threads for I/O operations like market data calls.
 * Handles dividend calculations, corporate actions, and equity P&L.
 */
@Slf4j
@Service
public class EquityCalculationEngineImpl implements EquityCalculationEngine {

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;

    @Autowired
    private MarketDataService marketDataService;

    @Override
    public List<CashFlowResponse.CashFlow> calculateDividendFlows(
            CashFlowRequestContent.Contract contract,
            CashFlowRequestContent.DateRange dateRange,
            CashFlowRequestContent request) {

        log.debug("Calculating dividend flows for contract: {} from {} to {}",
            contract.getContractId(), dateRange.getFromDate(), dateRange.getToDate());

        List<CashFlowResponse.CashFlow> dividendFlows = new ArrayList<>();

        // Get equity leg configuration
        CashFlowRequestContent.EquityLeg equityLeg = contract.getEquityLeg();
        if (equityLeg == null) {
            log.debug("No equity leg found for contract: {}", contract.getContractId());
            return dividendFlows;
        }

        // Process positions for dividend calculations
        if (request.getPositions() != null) {
            for (CashFlowRequestContent.Position position : request.getPositions()) {
                if (position.getContractId().equals(contract.getContractId())) {
                    List<CashFlowResponse.CashFlow> positionDividends = calculatePositionDividendFlows(
                        position, contract, dateRange, request);
                    dividendFlows.addAll(positionDividends);
                }
            }
        }

        log.debug("Generated {} dividend flows for contract: {}", dividendFlows.size(), contract.getContractId());
        return dividendFlows;
    }

    @Override
    public double calculatePositionDividend(
            CashFlowRequestContent.Position position,
            CashFlowRequestContent.Contract contract,
            LocalDate date,
            CashFlowRequestContent request) {

        // Get dividend rate for the underlying
        double dividendRate = getDividendRate(position.getUnderlying(), date, request);
        
        // Calculate dividend based on position quantity
        double dividendAmount = position.getQuantity() * dividendRate;
        
        // Apply withholding tax if applicable
        double withholdingTax = calculateWithholdingTax(dividendAmount, position.getCurrency());
        double netDividend = dividendAmount - withholdingTax;
        
        return Math.round(netDividend * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public double calculateEquityPnL(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        if (isLotClosed(lot)) {
            return calculateRealizedPnL(lot, request);
        } else {
            return calculateUnrealizedPnL(lot, request);
        }
    }

    @Override
    public double calculatePositionEquityPnL(CashFlowRequestContent.Position position, CashFlowRequestContent request) {
        double totalPnL = 0.0;
        
        // Calculate P&L for all lots in the position
        if (request.getLots() != null) {
            for (CashFlowRequestContent.Lot lot : request.getLots()) {
                if (lot.getPositionId().equals(position.getPositionId())) {
                    totalPnL += calculateEquityPnL(lot, request);
                }
            }
        }
        
        return Math.round(totalPnL * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public List<CashFlowResponse.CashFlow> calculateCorporateActionFlows(
            CashFlowRequestContent.Contract contract,
            CashFlowRequestContent.DateRange dateRange,
            CashFlowRequestContent request) {

        log.debug("Calculating corporate action flows for contract: {} from {} to {}",
            contract.getContractId(), dateRange.getFromDate(), dateRange.getToDate());

        List<CashFlowResponse.CashFlow> corporateActionFlows = new ArrayList<>();

        // Process corporate actions if any
        if (request.getCorporateActions() != null) {
            for (CashFlowRequestContent.CorporateAction corporateAction : request.getCorporateActions()) {
                if (isCorporateActionInRange(corporateAction, dateRange)) {
                    CashFlowResponse.CashFlow corporateActionFlow = createCorporateActionCashFlow(
                        contract, corporateAction, request);
                    corporateActionFlows.add(corporateActionFlow);
                }
            }
        }

        log.debug("Generated {} corporate action flows for contract: {}", 
            corporateActionFlows.size(), contract.getContractId());
        return corporateActionFlows;
    }

    @Override
    public double getCurrentMarketPrice(String instrumentId, LocalDate date, CashFlowRequestContent request) {
        return marketDataService.getPriceWithFallback(instrumentId, date, 0.0, request);
    }

    @Override
    public double getDividendRate(String underlier, LocalDate date, CashFlowRequestContent request) {
        return marketDataService.getDividendRateWithFallback(underlier, date, 0.02, request); // Default 2%
    }

    @Override
    public double calculateBasketPrice(CashFlowRequestContent.Contract contract, LocalDate date, CashFlowRequestContent request) {
        if (contract.getEquityLeg() == null || contract.getEquityLeg().getBasketWeights() == null) {
            return 0.0;
        }

        double weightedPrice = 0.0;
        for (CashFlowRequestContent.BasketWeight weight : contract.getEquityLeg().getBasketWeights()) {
            double price = getCurrentMarketPrice(weight.getInstrumentId(), date, request);
            weightedPrice += price * weight.getWeight();
        }

        return Math.round(weightedPrice * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public boolean isLotClosed(CashFlowRequestContent.Lot lot) {
        return lot.getCloseDate() != null;
    }

    @Override
    public double calculateRealizedPnL(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        if (!isLotClosed(lot)) {
            return 0.0;
        }

        double realizedPnL = (lot.getClosePrice() - lot.getCostBasis()) * lot.getQuantity();
        return Math.round(realizedPnL * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public double calculateUnrealizedPnL(CashFlowRequestContent.Lot lot, CashFlowRequestContent request) {
        if (isLotClosed(lot)) {
            return 0.0;
        }

        // Get current market price
        double currentPrice = getCurrentMarketPrice(lot.getInstrumentId(), LocalDate.now(), request);
        double unrealizedPnL = (currentPrice - lot.getCostBasis()) * lot.getQuantity();
        
        return Math.round(unrealizedPnL * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public CashFlowRequestContent.Position applyCorporateAction(
            CashFlowRequestContent.Position position,
            CorporateAction corporateAction) {

        // Create a copy of the position with corporate action adjustments
        CashFlowRequestContent.Position adjustedPosition = CashFlowRequestContent.Position.builder()
            .positionId(position.getPositionId())
            .contractId(position.getContractId())
            .underlying(position.getUnderlying())
            .quantity(calculateAdjustedQuantity(position.getQuantity(), corporateAction))
            .averagePrice(calculateAdjustedPrice(position.getAveragePrice(), corporateAction))
            .currency(position.getCurrency())
            .positionDate(position.getPositionDate())
            .book(position.getBook())
            .account(position.getAccount())
            .build();

        return adjustedPosition;
    }

    // Helper methods

    private List<CashFlowResponse.CashFlow> calculatePositionDividendFlows(
            CashFlowRequestContent.Position position,
            CashFlowRequestContent.Contract contract,
            CashFlowRequestContent.DateRange dateRange,
            CashFlowRequestContent request) {

        List<CashFlowResponse.CashFlow> dividendFlows = new ArrayList<>();

        // Check payment schedules for dividend dates
        if (request.getPaymentSchedules() != null) {
            for (CashFlowRequestContent.PaymentSchedule schedule : request.getPaymentSchedules()) {
                if (schedule.getContractId().equals(contract.getContractId()) &&
                    schedule.getPaymentType().equals("DIVIDEND") &&
                    isDateInRange(schedule.getPaymentDate(), dateRange)) {
                    
                    double dividendAmount = calculatePositionDividend(position, contract, schedule.getPaymentDate(), request);
                    
                    if (dividendAmount > 0) {
                        // Create accrual cash flow
                        CashFlowResponse.CashFlow accrualFlow = createDividendCashFlow(
                            contract, position, schedule, dividendAmount, "ACCRUAL", request);
                        dividendFlows.add(accrualFlow);

                        // Create payment cash flow
                        CashFlowResponse.CashFlow paymentFlow = createDividendCashFlow(
                            contract, position, schedule, dividendAmount, "REALIZED_UNSETTLED", request);
                        paymentFlow.setSettlementDate(calculateSettlementDate(schedule.getPaymentDate()));
                        dividendFlows.add(paymentFlow);
                    }
                }
            }
        }

        return dividendFlows;
    }

    private double calculateWithholdingTax(double dividendAmount, String currency) {
        // Simplified withholding tax calculation
        // In a real implementation, this would consider tax treaties and rates
        double withholdingRate = 0.15; // 15% default withholding rate
        return dividendAmount * withholdingRate;
    }

    private boolean isCorporateActionInRange(CashFlowRequestContent.CorporateAction corporateAction, 
                                          CashFlowRequestContent.DateRange dateRange) {
        LocalDate effectiveDate = corporateAction.getEffectiveDate();
        return !effectiveDate.isBefore(dateRange.getFromDate()) && 
               !effectiveDate.isAfter(dateRange.getToDate());
    }

    private boolean isDateInRange(LocalDate date, CashFlowRequestContent.DateRange dateRange) {
        return !date.isBefore(dateRange.getFromDate()) && !date.isAfter(dateRange.getToDate());
    }

    private double calculateAdjustedQuantity(double originalQuantity, CorporateAction corporateAction) {
        switch (corporateAction.getActionType()) {
            case "SPLIT":
                return originalQuantity * corporateAction.getRatio();
            case "MERGER":
                return originalQuantity * corporateAction.getRatio();
            case "SPINOFF":
                return originalQuantity; // Original position remains
            default:
                return originalQuantity;
        }
    }

    private double calculateAdjustedPrice(double originalPrice, CorporateAction corporateAction) {
        switch (corporateAction.getActionType()) {
            case "SPLIT":
                return originalPrice / corporateAction.getRatio();
            case "MERGER":
                return originalPrice * corporateAction.getRatio();
            case "SPINOFF":
                return originalPrice; // Price remains the same
            default:
                return originalPrice;
        }
    }

    private LocalDate calculateSettlementDate(LocalDate paymentDate) {
        // T+2 settlement for most dividend payments
        return paymentDate.plusDays(2);
    }

    private CashFlowResponse.CashFlow createDividendCashFlow(
            CashFlowRequestContent.Contract contract,
            CashFlowRequestContent.Position position,
            CashFlowRequestContent.PaymentSchedule schedule,
            double amount,
            String status,
            CashFlowRequestContent request) {

        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("DIV_" + UUID.randomUUID().toString())
            .contractId(contract.getContractId())
            .positionId(position.getPositionId())
            .cashFlowType("DIVIDEND")
            .cashFlowDate(schedule.getPaymentDate())
            .amount(amount)
            .currency(contract.getCurrency())
            .status(status)
            .calculationBasis("SCHEDULED")
            .build();
    }

    private CashFlowResponse.CashFlow createCorporateActionCashFlow(
            CashFlowRequestContent.Contract contract,
            CashFlowRequestContent.CorporateAction corporateAction,
            CashFlowRequestContent request) {

        return CashFlowResponse.CashFlow.builder()
            .cashFlowId("CA_" + UUID.randomUUID().toString())
            .contractId(contract.getContractId())
            .cashFlowType("CORPORATE_ACTION")
            .cashFlowDate(corporateAction.getEffectiveDate())
            .amount(0.0) // Corporate actions typically don't generate cash flows
            .currency(contract.getCurrency())
            .status("PROCESSED")
            .calculationBasis("EVENT_DRIVEN")
            .metadata("Corporate Action: " + corporateAction.getActionType() + " - " + corporateAction.getDescription())
            .build();
    }
}
