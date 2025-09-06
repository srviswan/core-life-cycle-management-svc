package com.financial.cashflow.exception;

import com.financial.cashflow.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global Exception Handler for Cash Flow Management Service
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle cash flow calculation exceptions
     */
    @ExceptionHandler(CashFlowCalculationException.class)
    public ResponseEntity<ErrorResponse> handleCalculationException(CashFlowCalculationException e, WebRequest request) {
        log.error("Calculation error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("CALCULATION_ERROR")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle market data exceptions
     */
    @ExceptionHandler(MarketDataException.class)
    public ResponseEntity<ErrorResponse> handleMarketDataException(MarketDataException e, WebRequest request) {
        log.error("Market data error", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.builder()
                .errorCode("MARKET_DATA_ERROR")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e, WebRequest request) {
        log.error("Validation error", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle data persistence exceptions
     */
    @ExceptionHandler(DataPersistenceException.class)
    public ResponseEntity<ErrorResponse> handleDataPersistenceException(DataPersistenceException e, WebRequest request) {
        log.error("Data persistence error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("DATA_PERSISTENCE_ERROR")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle data retrieval exceptions
     */
    @ExceptionHandler(DataRetrievalException.class)
    public ResponseEntity<ErrorResponse> handleDataRetrievalException(DataRetrievalException e, WebRequest request) {
        log.error("Data retrieval error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("DATA_RETRIEVAL_ERROR")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle calculation engine exceptions
     */
    @ExceptionHandler(CalculationException.class)
    public ResponseEntity<ErrorResponse> handleCalculationEngineException(CalculationException e, WebRequest request) {
        log.error("Calculation engine error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("CALCULATION_ENGINE_ERROR")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle method argument validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, WebRequest request) {
        log.error("Method argument validation error", e);
        
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            validationErrors.add(ErrorResponse.ValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .validationErrors(validationErrors)
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        log.error("Illegal argument error", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode("INVALID_ARGUMENT")
                .message(e.getMessage())
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, WebRequest request) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build());
    }
}
