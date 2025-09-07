package com.financial.cashflow.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load Testing Framework for Cash Flow Management Service
 * Tests different thread pool configurations and load scenarios
 */
@Slf4j
public class LoadTestFramework {
    
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public LoadTestFramework(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Enable Java 8 time support
    }
    
    /**
     * Load test result containing performance metrics
     */
    public static class LoadTestResult {
        private final String testName;
        private final int threadCount;
        private final int totalRequests;
        private final long totalTimeMs;
        private final long avgResponseTimeMs;
        private final long minResponseTimeMs;
        private final long maxResponseTimeMs;
        private final int successCount;
        private final int errorCount;
        private final double throughputPerSecond;
        private final List<Long> responseTimes;
        
        public LoadTestResult(String testName, int threadCount, int totalRequests, long totalTimeMs,
                            long avgResponseTimeMs, long minResponseTimeMs, long maxResponseTimeMs,
                            int successCount, int errorCount, double throughputPerSecond,
                            List<Long> responseTimes) {
            this.testName = testName;
            this.threadCount = threadCount;
            this.totalRequests = totalRequests;
            this.totalTimeMs = totalTimeMs;
            this.avgResponseTimeMs = avgResponseTimeMs;
            this.minResponseTimeMs = minResponseTimeMs;
            this.maxResponseTimeMs = maxResponseTimeMs;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.throughputPerSecond = throughputPerSecond;
            this.responseTimes = responseTimes;
        }
        
        // Getters
        public String getTestName() { return testName; }
        public int getThreadCount() { return threadCount; }
        public int getTotalRequests() { return totalRequests; }
        public long getTotalTimeMs() { return totalTimeMs; }
        public long getAvgResponseTimeMs() { return avgResponseTimeMs; }
        public long getMinResponseTimeMs() { return minResponseTimeMs; }
        public long getMaxResponseTimeMs() { return maxResponseTimeMs; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public double getThroughputPerSecond() { return throughputPerSecond; }
        public List<Long> getResponseTimes() { return responseTimes; }
        
        @Override
        public String toString() {
            return String.format(
                "LoadTestResult{testName='%s', threadCount=%d, totalRequests=%d, totalTimeMs=%d, " +
                "avgResponseTimeMs=%d, minResponseTimeMs=%d, maxResponseTimeMs=%d, " +
                "successCount=%d, errorCount=%d, throughputPerSecond=%.2f}",
                testName, threadCount, totalRequests, totalTimeMs, avgResponseTimeMs,
                minResponseTimeMs, maxResponseTimeMs, successCount, errorCount, throughputPerSecond
            );
        }
    }
    
    /**
     * Run load test with specified parameters
     */
    public LoadTestResult runLoadTest(String testName, CashFlowRequest request, 
                                    int threadCount, int totalRequests, int rampUpSeconds) {
        log.info("Starting load test: {} with {} threads, {} requests, {}s ramp-up", 
                testName, threadCount, totalRequests, rampUpSeconds);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        // Create request payload
        String requestJson = createRequestJson(request);
        
        // Submit tasks
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();
                    
                    // Add ramp-up delay
                    if (rampUpSeconds > 0) {
                        Thread.sleep((long) (requestId * 1000.0 * rampUpSeconds / totalRequests));
                    }
                    
                    long startTime = System.currentTimeMillis();
                    
                    // Make request
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
                    
                    ResponseEntity<String> response = restTemplate.postForEntity(
                        baseUrl + "/api/v1/cashflows/calculate", entity, String.class);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("Request {} failed with status: {}", requestId, response.getStatusCode());
                    }
                    
                    responseTimes.add(responseTime);
                    totalResponseTime.addAndGet(responseTime);
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("Request {} failed with exception", requestId, e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start the test
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        try {
            // Wait for all requests to complete
            boolean completed = completionLatch.await(300, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Load test did not complete within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Load test interrupted", e);
        }
        
        long testEndTime = System.currentTimeMillis();
        long totalTimeMs = testEndTime - testStartTime;
        
        // Calculate metrics
        long avgResponseTime = responseTimes.isEmpty() ? 0 : totalResponseTime.get() / responseTimes.size();
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double throughputPerSecond = totalRequests * 1000.0 / totalTimeMs;
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LoadTestResult result = new LoadTestResult(
            testName, threadCount, totalRequests, totalTimeMs, avgResponseTime,
            minResponseTime, maxResponseTime, successCount.get(), errorCount.get(),
            throughputPerSecond, new ArrayList<>(responseTimes)
        );
        
        log.info("Load test completed: {}", result);
        return result;
    }
    
    /**
     * Run thread pool sizing test with different thread counts
     */
    public List<LoadTestResult> runThreadPoolSizingTest(CashFlowRequest request, 
                                                       int[] threadCounts, 
                                                       int requestsPerThreadCount,
                                                       int rampUpSeconds) {
        List<LoadTestResult> results = new ArrayList<>();
        
        for (int threadCount : threadCounts) {
            String testName = String.format("ThreadPool_%d_Threads", threadCount);
            LoadTestResult result = runLoadTest(testName, request, threadCount, 
                                              requestsPerThreadCount, rampUpSeconds);
            results.add(result);
            
            // Wait between tests to let system stabilize
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Create JSON request from CashFlowRequest object
     */
    private String createRequestJson(CashFlowRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request JSON", e);
        }
    }
    
    /**
     * Print load test results in a formatted table
     */
    public static void printResultsTable(List<LoadTestResult> results) {
        System.out.println("\n" + "=".repeat(120));
        System.out.println("LOAD TEST RESULTS SUMMARY");
        System.out.println("=".repeat(120));
        System.out.printf("%-20s %-8s %-8s %-12s %-12s %-12s %-12s %-8s %-8s %-12s%n",
            "Test Name", "Threads", "Requests", "Total Time", "Avg Resp", "Min Resp", "Max Resp", 
            "Success", "Errors", "Throughput");
        System.out.println("-".repeat(120));
        
        for (LoadTestResult result : results) {
            System.out.printf("%-20s %-8d %-8d %-12d %-12d %-12d %-12d %-8d %-8d %-12.2f%n",
                result.getTestName(), result.getThreadCount(), result.getTotalRequests(),
                result.getTotalTimeMs(), result.getAvgResponseTimeMs(), result.getMinResponseTimeMs(),
                result.getMaxResponseTimeMs(), result.getSuccessCount(), result.getErrorCount(),
                result.getThroughputPerSecond());
        }
        
        System.out.println("=".repeat(120));
        
        // Find optimal thread count
        LoadTestResult optimal = results.stream()
            .filter(r -> r.getErrorCount() == 0)
            .max((r1, r2) -> Double.compare(r1.getThroughputPerSecond(), r2.getThroughputPerSecond()))
            .orElse(null);
        
        if (optimal != null) {
            System.out.printf("OPTIMAL THREAD COUNT: %d threads (%.2f req/sec throughput)%n", 
                optimal.getThreadCount(), optimal.getThroughputPerSecond());
        } else {
            System.out.println("No optimal thread count found (all tests had errors)");
        }
        System.out.println("=".repeat(120));
    }
}
