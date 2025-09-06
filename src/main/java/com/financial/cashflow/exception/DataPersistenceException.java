package com.financial.cashflow.exception;

/**
 * Exception for data persistence errors
 */
public class DataPersistenceException extends RuntimeException {
    
    public DataPersistenceException(String message) {
        super(message);
    }
    
    public DataPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
