package com.financial.cashflow.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health indicator for Cash Flow Management Service
 * Simplified version without Spring Boot Actuator dependencies
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CashFlowHealthIndicator {
    
    private final DataSource dataSource;
    
    /**
     * Check database connectivity
     */
    public boolean isHealthy() {
        try {
            try (Connection conn = dataSource.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }
    
    /**
     * Get health status message
     */
    public String getHealthStatus() {
        if (isHealthy()) {
            return "Cash Flow Management Service is healthy - Database connection OK";
        } else {
            return "Cash Flow Management Service is unhealthy - Database connection failed";
        }
    }
}
