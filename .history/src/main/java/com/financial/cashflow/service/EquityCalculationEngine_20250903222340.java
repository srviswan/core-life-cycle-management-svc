package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Specialized engine for equity calculations.
 * Handles complex equity scenarios including:
 * - Dividend calculations and payments
 * - Corporate actions (splits, mergers, spin-offs)
 * - Equity P&L calculations
 * - Basket and index calculations
 * - Dividend reinvestment vs cash payment
 */
public interface EquityCalculationEngine {

    /**
     * Calculate dividend cash flows for equity positions.
     * 
     * @param contract The contract containing equity leg details
     * @param dateRange The date range for calculations
     * @param request The full request context
     * @return List of dividend cash flows
     */
    List<CashFlowResponse.CashFlow> calculateDividendFlows(
        CashFlowRequestContent.Contract contract,
        CashFlowRequestContent.DateRange dateRange,
        CashFlowRequestContent request
    );

    /**
     * Calculate dividend for a specific position.
     * 
     * @param position The position
     * @param contract The contract
     * @param date The dividend date
     * @param request The full request context
     * @return Dividend amount
     */
    double calculatePositionDividend(
        CashFlowRequestContent.Position position,
        CashFlowRequestContent.Contract contract,
        LocalDate date,
        CashFlowRequestContent request
    );

    /**
     * Calculate equity P&L for a lot.
     * 
     * @param lot The lot
     * @param request The full request context
     * @return P&L amount
     */
    double calculateEquityPnL(
        CashFlowRequestContent.Lot lot,
        CashFlowRequestContent request
    );

    /**
     * Calculate equity P&L for a position.
     * 
     * @param position The position
     * @param request The full request context
     * @return P&L amount
     */
    double calculatePositionEquityPnL(
        CashFlowRequestContent.Position position,
        CashFlowRequestContent request
    );

    /**
     * Calculate corporate action cash flows.
     * 
     * @param contract The contract
     * @param dateRange The date range
     * @param request The full request context
     * @return List of corporate action cash flows
     */
    List<CashFlowResponse.CashFlow> calculateCorporateActionFlows(
        CashFlowRequestContent.Contract contract,
        CashFlowRequestContent.DateRange dateRange,
        CashFlowRequestContent request
    );

    /**
     * Get current market price for an instrument.
     * 
     * @param instrumentId The instrument ID
     * @param date The date for price
     * @param request The full request context
     * @return Current market price
     */
    double getCurrentMarketPrice(
        String instrumentId,
        LocalDate date,
        CashFlowRequestContent request
    );

    /**
     * Get dividend rate for an underlier.
     * 
     * @param underlier The underlier
     * @param date The date
     * @param request The full request context
     * @return Dividend rate
     */
    double getDividendRate(
        String underlier,
        LocalDate date,
        CashFlowRequestContent request
    );

    /**
     * Calculate basket-weighted price for index/basket contracts.
     * 
     * @param contract The contract
     * @param date The date
     * @param request The full request context
     * @return Weighted price
     */
    double calculateBasketPrice(
        CashFlowRequestContent.Contract contract,
        LocalDate date,
        CashFlowRequestContent request
    );

    /**
     * Check if a lot is closed.
     * 
     * @param lot The lot
     * @return True if closed, false otherwise
     */
    boolean isLotClosed(CashFlowRequestContent.Lot lot);

    /**
     * Calculate realized P&L for a closed lot.
     * 
     * @param lot The lot
     * @param request The full request context
     * @return Realized P&L
     */
    double calculateRealizedPnL(
        CashFlowRequestContent.Lot lot,
        CashFlowRequestContent request
    );

    /**
     * Calculate unrealized P&L for an open lot.
     * 
     * @param lot The lot
     * @param request The full request context
     * @return Unrealized P&L
     */
    double calculateUnrealizedPnL(
        CashFlowRequestContent.Lot lot,
        CashFlowRequestContent request
    );

    /**
     * Apply corporate action adjustments to positions.
     * 
     * @param position The position
     * @param corporateAction The corporate action
     * @return Adjusted position
     */
    CashFlowRequestContent.Position applyCorporateAction(
        CashFlowRequestContent.Position position,
        CorporateAction corporateAction
    );

    /**
     * Represents a corporate action.
     */
    class CorporateAction {
        private final String actionType; // SPLIT, MERGER, SPINOFF, RIGHTS
        private final String instrumentId;
        private final LocalDate effectiveDate;
        private final double ratio;
        private final String description;

        public CorporateAction(String actionType, String instrumentId, LocalDate effectiveDate, 
                             double ratio, String description) {
            this.actionType = actionType;
            this.instrumentId = instrumentId;
            this.effectiveDate = effectiveDate;
            this.ratio = ratio;
            this.description = description;
        }

        public String getActionType() { return actionType; }
        public String getInstrumentId() { return instrumentId; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public double getRatio() { return ratio; }
        public String getDescription() { return description; }
    }
}
