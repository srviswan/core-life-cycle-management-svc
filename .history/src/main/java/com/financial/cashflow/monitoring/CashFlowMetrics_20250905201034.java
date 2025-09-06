package com.financial.cashflow.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for Cash Flow Management Service
 */
@Component
@Slf4j
public class CashFlowMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter calculationCounter;
    private final Timer calculationTimer;
    private final Gauge activeCalculationsGauge;
    private final Counter marketDataCounter;
    private final Timer marketDataTimer;
    private final Counter errorCounter;
    
    private final AtomicInteger activeCalculations = new AtomicInteger(0);
    private final AtomicLong totalCalculations = new AtomicLong(0);
    
    public CashFlowMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Calculation metrics
        this.calculationCounter = Counter.builder("cashflow.calculations.total")
            .description("Total number of cash flow calculations")
            .register(meterRegistry);
            
        this.calculationTimer = Timer.builder("cashflow.calculations.duration")
            .description("Duration of cash flow calculations")
            .register(meterRegistry);
            
        this.activeCalculationsGauge = Gauge.builder("cashflow.calculations.active")
            .description("Number of active calculations")
            .register(meterRegistry, this, CashFlowMetrics::getActiveCalculations);
        
        // Market data metrics
        this.marketDataCounter = Counter.builder("cashflow.marketdata.requests.total")
            .description("Total number of market data requests")
            .register(meterRegistry);
            
        this.marketDataTimer = Timer.builder("cashflow.marketdata.requests.duration")
            .description("Duration of market data requests")
            .register(meterRegistry);
        
        // Error metrics
        this.errorCounter = Counter.builder("cashflow.errors.total")
            .description("Total number of errors")
            .register(meterRegistry);
    }
    
    /**
     * Record calculation metrics
     */
    public void recordCalculation(String calculationType, long durationMs) {
        calculationCounter.increment();
        calculationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalCalculations.incrementAndGet();
        
        meterRegistry.counter("cashflow.calculations.by.type", "type", calculationType).increment();
        
        log.debug("Recorded calculation metrics: type={}, duration={}ms", calculationType, durationMs);
    }
    
    /**
     * Record market data metrics
     */
    public void recordMarketDataRequest(String source, long durationMs) {
        marketDataCounter.increment();
        marketDataTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("cashflow.marketdata.by.source", "source", source).increment();
        
        log.debug("Recorded market data metrics: source={}, duration={}ms", source, durationMs);
    }
    
    /**
     * Record error metrics
     */
    public void recordError(String errorType, String errorCode) {
        errorCounter.increment();
        
        meterRegistry.counter("cashflow.errors.by.type", "type", errorType).increment();
        meterRegistry.counter("cashflow.errors.by.code", "code", errorCode).increment();
        
        log.debug("Recorded error metrics: type={}, code={}", errorType, errorCode);
    }
    
    /**
     * Increment active calculations
     */
    public void incrementActiveCalculations() {
        activeCalculations.incrementAndGet();
    }
    
    /**
     * Decrement active calculations
     */
    public void decrementActiveCalculations() {
        activeCalculations.decrementAndGet();
    }
    
    /**
     * Get active calculations count
     */
    private double getActiveCalculations() {
        return activeCalculations.get();
    }
    
    /**
     * Get total calculations count
     */
    public long getTotalCalculations() {
        return totalCalculations.get();
    }
}
