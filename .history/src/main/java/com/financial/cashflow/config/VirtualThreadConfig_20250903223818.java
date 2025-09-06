package com.financial.cashflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for virtual threads and platform threads.
 * 
 * Virtual threads are used for I/O-bound operations:
 * - Database queries
 * - HTTP calls to external APIs
 * - File I/O operations
 * - Network operations
 * 
 * Platform threads are used for CPU-intensive operations:
 * - Complex calculations
 * - Algorithm execution
 * - Data processing
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * Virtual thread executor for I/O-bound operations.
     * Use for:
     * - Market data API calls
     * - Database operations
     * - File I/O
     * - Network operations
     */
    @Bean("virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Platform thread executor for CPU-intensive operations.
     * Use for:
     * - Complex calculations
     * - Algorithm execution
     * - Data processing
     */
    @Bean("cpuThreadExecutor")
    public ExecutorService cpuThreadExecutor() {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    }

    /**
     * Mixed thread executor for workloads that combine I/O and CPU operations.
     * Use for:
     * - Parallel processing with both I/O and CPU work
     * - Mixed workloads
     */
    @Bean("mixedThreadExecutor")
    public ExecutorService mixedThreadExecutor() {
        return Executors.newWorkStealingPool();
    }
}
