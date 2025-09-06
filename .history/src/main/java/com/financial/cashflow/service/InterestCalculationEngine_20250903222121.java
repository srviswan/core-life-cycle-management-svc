package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Specialized engine for interest calculations.
 * Handles complex interest scenarios including:
 * - Different day count conventions (ACT/360, 30/360, ACT/365)
 * - Interest rate resets and floating rates
 * - Compounding periods and frequency
 * - Business day adjustments
 * - Holiday calendar considerations
 */
public interface InterestCalculationEngine {

    /**
     * Calculate interest cash flows for a contract over a date range.
     * 
     * @param contract The contract containing interest leg details
     * @param dateRange The date range for calculations
     * @param request The full request context
     * @return List of interest cash flows
     */
    List<CashFlowResponse.CashFlow> calculateInterestFlows(
        CashFlowRequestContent.Contract contract,
        CashFlowRequestContent.DateRange dateRange,
        CashFlowRequestContent request
    );

    /**
     * Calculate interest for a specific period.
     * 
     * @param contract The contract
     * @param startDate Period start date
     * @param endDate Period end date
     * @param dayCountConvention Day count convention to use
     * @param request The full request context
     * @return Interest amount for the period
     */
    double calculatePeriodInterest(
        CashFlowRequestContent.Contract contract,
        LocalDate startDate,
        LocalDate endDate,
        String dayCountConvention,
        CashFlowRequestContent request
    );

    /**
     * Get the effective interest rate for a specific date.
     * 
     * @param contract The contract
     * @param date The date for rate calculation
     * @param request The full request context
     * @return Effective interest rate
     */
    double getEffectiveRate(
        CashFlowRequestContent.Contract contract,
        LocalDate date,
        CashFlowRequestContent request
    );

    /**
     * Calculate the number of days between two dates using specified convention.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @param dayCountConvention Day count convention
     * @return Number of days
     */
    int calculateDays(LocalDate startDate, LocalDate endDate, String dayCountConvention);

    /**
     * Get the day count denominator for a convention.
     * 
     * @param dayCountConvention The day count convention
     * @return Denominator (360, 365, etc.)
     */
    int getDayCountDenominator(String dayCountConvention);

    /**
     * Check if a date is a business day.
     * 
     * @param date The date to check
     * @return True if business day, false otherwise
     */
    boolean isBusinessDay(LocalDate date);

    /**
     * Calculate interest periods based on payment frequency.
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @param paymentFrequency Payment frequency
     * @param resetFrequency Reset frequency
     * @return List of interest periods
     */
    List<InterestPeriod> calculateInterestPeriods(
        LocalDate fromDate,
        LocalDate toDate,
        String paymentFrequency,
        String resetFrequency
    );

    /**
     * Represents an interest calculation period.
     */
    class InterestPeriod {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String resetType;

        public InterestPeriod(LocalDate startDate, LocalDate endDate) {
            this(startDate, endDate, "REGULAR");
        }

        public InterestPeriod(LocalDate startDate, LocalDate endDate, String resetType) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.resetType = resetType;
        }

        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public String getResetType() { return resetType; }
    }
}
