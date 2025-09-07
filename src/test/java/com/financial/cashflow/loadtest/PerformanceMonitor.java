package com.financial.cashflow.loadtest;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance Monitor for Load Testing
 * Tracks JVM metrics during load tests
 */
@Slf4j
public class PerformanceMonitor {
    
    private final ScheduledExecutorService scheduler;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private volatile boolean monitoring = false;
    
    private long startTime;
    private long maxMemoryUsed = 0;
    private int maxThreadCount = 0;
    private long totalGcTime = 0;
    
    public PerformanceMonitor() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }
    
    /**
     * Start performance monitoring
     */
    public void startMonitoring() {
        log.info("Starting performance monitoring");
        monitoring = true;
        startTime = System.currentTimeMillis();
        
        // Reset metrics
        maxMemoryUsed = 0;
        maxThreadCount = 0;
        totalGcTime = 0;
        
        // Schedule monitoring task
        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Stop performance monitoring and return summary
     */
    public PerformanceSummary stopMonitoring() {
        log.info("Stopping performance monitoring");
        monitoring = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        long monitoringDuration = System.currentTimeMillis() - startTime;
        
        return new PerformanceSummary(
            monitoringDuration,
            maxMemoryUsed,
            maxThreadCount,
            totalGcTime,
            getCurrentMemoryUsage(),
            getCurrentThreadCount()
        );
    }
    
    /**
     * Collect current metrics
     */
    private void collectMetrics() {
        if (!monitoring) return;
        
        // Memory usage
        long currentMemoryUsed = getCurrentMemoryUsage();
        if (currentMemoryUsed > maxMemoryUsed) {
            maxMemoryUsed = currentMemoryUsed;
        }
        
        // Thread count
        int currentThreadCount = getCurrentThreadCount();
        if (currentThreadCount > maxThreadCount) {
            maxThreadCount = currentThreadCount;
        }
        
        // GC time
        totalGcTime = getTotalGcTime();
    }
    
    /**
     * Get current memory usage in MB
     */
    private long getCurrentMemoryUsage() {
        return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
    
    /**
     * Get current thread count
     */
    private int getCurrentThreadCount() {
        return threadBean.getThreadCount();
    }
    
    /**
     * Get total GC time in milliseconds
     */
    private long getTotalGcTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(gc -> gc.getCollectionTime())
            .sum();
    }
    
    /**
     * Performance summary containing key metrics
     */
    public static class PerformanceSummary {
        private final long monitoringDurationMs;
        private final long maxMemoryUsedMB;
        private final int maxThreadCount;
        private final long totalGcTimeMs;
        private final long finalMemoryUsedMB;
        private final int finalThreadCount;
        
        public PerformanceSummary(long monitoringDurationMs, long maxMemoryUsedMB, int maxThreadCount,
                                long totalGcTimeMs, long finalMemoryUsedMB, int finalThreadCount) {
            this.monitoringDurationMs = monitoringDurationMs;
            this.maxMemoryUsedMB = maxMemoryUsedMB;
            this.maxThreadCount = maxThreadCount;
            this.totalGcTimeMs = totalGcTimeMs;
            this.finalMemoryUsedMB = finalMemoryUsedMB;
            this.finalThreadCount = finalThreadCount;
        }
        
        // Getters
        public long getMonitoringDurationMs() { return monitoringDurationMs; }
        public long getMaxMemoryUsedMB() { return maxMemoryUsedMB; }
        public int getMaxThreadCount() { return maxThreadCount; }
        public long getTotalGcTimeMs() { return totalGcTimeMs; }
        public long getFinalMemoryUsedMB() { return finalMemoryUsedMB; }
        public int getFinalThreadCount() { return finalThreadCount; }
        
        @Override
        public String toString() {
            return String.format(
                "PerformanceSummary{duration=%dms, maxMemory=%dMB, maxThreads=%d, " +
                "totalGcTime=%dms, finalMemory=%dMB, finalThreads=%d}",
                monitoringDurationMs, maxMemoryUsedMB, maxThreadCount,
                totalGcTimeMs, finalMemoryUsedMB, finalThreadCount
            );
        }
        
        /**
         * Print formatted performance summary
         */
        public void printSummary() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PERFORMANCE MONITORING SUMMARY");
            System.out.println("=".repeat(60));
            System.out.printf("Monitoring Duration: %d ms (%.2f seconds)%n", 
                monitoringDurationMs, monitoringDurationMs / 1000.0);
            System.out.printf("Max Memory Used: %d MB%n", maxMemoryUsedMB);
            System.out.printf("Max Thread Count: %d%n", maxThreadCount);
            System.out.printf("Total GC Time: %d ms (%.2f seconds)%n", 
                totalGcTimeMs, totalGcTimeMs / 1000.0);
            System.out.printf("Final Memory Used: %d MB%n", finalMemoryUsedMB);
            System.out.printf("Final Thread Count: %d%n", finalThreadCount);
            System.out.printf("GC Overhead: %.2f%%%n", 
                (totalGcTimeMs * 100.0) / monitoringDurationMs);
            System.out.println("=".repeat(60));
        }
    }
}
