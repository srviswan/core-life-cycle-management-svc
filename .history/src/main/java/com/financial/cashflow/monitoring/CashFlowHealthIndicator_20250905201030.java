package com.financial.cashflow.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health indicator for Cash Flow Management Service
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CashFlowHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    return Health.up()
                        .withDetail("database", "UP")
                        .withDetail("service", "Cash Flow Management Service")
                        .withDetail("status", "All systems operational")
                        .build();
                }
            }
            
            return Health.down()
                .withDetail("database", "DOWN")
                .withDetail("service", "Cash Flow Management Service")
                .withDetail("status", "Database connection failed")
                .build();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("service", "Cash Flow Management Service")
                .withDetail("status", "Health check failed")
                .build();
        }
    }
}
