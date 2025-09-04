package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;

import java.util.List;

/**
 * Engine for calculating cash flows.
 */
public interface CashFlowEngine {
    
    /**
     * Calculate cash flows for a given request.
     * 
     * @param request The cash flow calculation request
     * @return List of calculated cash flows
     */
    List<CashFlowResponse.CashFlow> calculateCashFlows(CashFlowRequestContent request);
}
