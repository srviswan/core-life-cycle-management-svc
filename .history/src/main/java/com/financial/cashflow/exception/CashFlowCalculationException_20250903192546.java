package com.financial.cashflow.exception;

/**
 * Exception thrown when cash flow calculation fails.
 */
public class CashFlowCalculationException extends RuntimeException {

    public CashFlowCalculationException(String message) {
        super(message);
    }

    public CashFlowCalculationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CashFlowCalculationException(Throwable cause) {
        super(cause);
    }
}
