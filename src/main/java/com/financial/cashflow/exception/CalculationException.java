package com.financial.cashflow.exception;

/**
 * Exception for calculation engine errors
 */
public class CalculationException extends RuntimeException {
    
    public CalculationException(String message) {
        super(message);
    }
    
    public CalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
