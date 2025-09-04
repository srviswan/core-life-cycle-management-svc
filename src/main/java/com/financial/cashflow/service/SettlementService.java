package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;

import java.util.List;

/**
 * Service for generating settlement instructions.
 */
public interface SettlementService {
    
    /**
     * Generate settlement instructions for cash flows.
     * 
     * @param cashFlows The calculated cash flows
     * @param request The original request
     * @return List of settlement instructions
     */
    List<CashFlowResponse.SettlementInstruction> generateSettlementInstructions(
        List<CashFlowResponse.CashFlow> cashFlows, 
        CashFlowRequestContent request);
}
