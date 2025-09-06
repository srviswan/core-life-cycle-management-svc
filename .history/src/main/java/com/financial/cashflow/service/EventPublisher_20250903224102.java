package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowResponse;

/**
 * Service for publishing events.
 */
public interface EventPublisher {
    
    /**
     * Publish cash flow calculated event.
     * 
     * @param response The cash flow calculation response
     */
    void publishCashFlowCalculated(CashFlowResponse response);
    
    /**
     * Publish cash flow calculated event with specific details.
     * 
     * @param requestId The request ID
     * @param contractId The contract ID
     * @param cashFlowCount The number of cash flows
     * @param totalAmount The total amount
     * @param currency The currency
     */
    void publishCashFlowCalculatedEvent(String requestId, String contractId, int cashFlowCount, Double totalAmount, String currency);
}
