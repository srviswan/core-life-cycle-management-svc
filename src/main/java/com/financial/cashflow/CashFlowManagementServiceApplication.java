package com.financial.cashflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for Cash Flow Management Service
 * Following conventional Spring Boot implementation pattern
 */
@SpringBootApplication
@EnableAsync
@Slf4j
public class CashFlowManagementServiceApplication {
    
    public static void main(String[] args) {
        log.info("Starting Cash Flow Management Service...");
        SpringApplication.run(CashFlowManagementServiceApplication.class, args);
        log.info("Cash Flow Management Service started successfully");
    }
}
