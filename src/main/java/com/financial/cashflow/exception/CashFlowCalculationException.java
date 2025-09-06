package com.financial.cashflow.exception;

/**
 * Exception for cash flow calculation errors
 */
public class CashFlowCalculationException extends RuntimeException {
    
    public CashFlowCalculationException(String message) {
        super(message);
    }
    
    public CashFlowCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
