package com.financial.cashflow.exception;

/**
 * Exception for market data related errors
 */
public class MarketDataException extends RuntimeException {
    
    public MarketDataException(String message) {
        super(message);
    }
    
    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
