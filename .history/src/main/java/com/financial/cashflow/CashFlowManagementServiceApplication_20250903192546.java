package com.financial.cashflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Cash Flow Management Service.
 * 
 * This service provides:
 * - Cash flow calculations for synthetic swaps
 * - Settlement instruction generation
 * - Real-time event publishing
 * - Caching for performance optimization
 * - Audit trail management
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class CashFlowManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashFlowManagementServiceApplication.class, args);
    }
}
