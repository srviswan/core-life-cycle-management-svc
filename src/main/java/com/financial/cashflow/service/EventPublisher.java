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
}
