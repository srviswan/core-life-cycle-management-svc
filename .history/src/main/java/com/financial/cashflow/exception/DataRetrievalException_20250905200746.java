package com.financial.cashflow.exception;

/**
 * Exception for data retrieval errors
 */
public class DataRetrievalException extends RuntimeException {
    
    public DataRetrievalException(String message) {
        super(message);
    }
    
    public DataRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
