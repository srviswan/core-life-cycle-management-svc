package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;

/**
 * Validator for cash flow calculation requests.
 */
public interface CashFlowValidator {
    
    /**
     * Validate a cash flow request.
     * 
     * @param request The request to validate
     * @return true if valid
     * @throws com.financial.cashflow.exception.ValidationException if validation fails
     */
    boolean validateRequest(CashFlowRequestContent request);
}
