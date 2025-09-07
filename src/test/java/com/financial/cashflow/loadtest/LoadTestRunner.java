package com.financial.cashflow.loadtest;

import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Load Test Runner for Cash Flow Management Service
 * Executes comprehensive load tests to determine optimal thread pool sizing
 */
@Slf4j
public class LoadTestRunner {
    
    private static final String BASE_URL = "http://localhost:8080";
    
    public static void main(String[] args) {
        log.info("Starting Cash Flow Management Service Load Tests");
        
        LoadTestFramework framework = new LoadTestFramework(BASE_URL);
        
        // Test different thread pool sizes
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64, 128};
        int requestsPerThreadCount = 50;
        int rampUpSeconds = 10;
        
        // Run tests for different complexity levels
        runComplexityTests(framework, threadCounts, requestsPerThreadCount, rampUpSeconds);
        
        // Run sustained load test
        runSustainedLoadTest(framework);
        
        // Run burst load test
        runBurstLoadTest(framework);
        
        log.info("Load testing completed");
    }
    
    /**
     * Run tests for different complexity levels
     */
    private static void runComplexityTests(LoadTestFramework framework, int[] threadCounts, 
                                         int requestsPerThreadCount, int rampUpSeconds) {
        log.info("Running complexity-based load tests");
        
        // Simple complexity test
        log.info("=== SIMPLE COMPLEXITY TEST ===");
        CashFlowRequest simpleRequest = TestDataGenerator.generateSimpleRequest();
        List<LoadTestFramework.LoadTestResult> simpleResults = framework.runThreadPoolSizingTest(
            simpleRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(simpleResults);
        
        // Medium complexity test
        log.info("=== MEDIUM COMPLEXITY TEST ===");
        CashFlowRequest mediumRequest = TestDataGenerator.generateMediumRequest();
        List<LoadTestFramework.LoadTestResult> mediumResults = framework.runThreadPoolSizingTest(
            mediumRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(mediumResults);
        
        // Complex complexity test
        log.info("=== COMPLEX COMPLEXITY TEST ===");
        CashFlowRequest complexRequest = TestDataGenerator.generateComplexRequest();
        List<LoadTestFramework.LoadTestResult> complexResults = framework.runThreadPoolSizingTest(
            complexRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(complexResults);
        
        // High complexity test
        log.info("=== HIGH COMPLEXITY TEST ===");
        CashFlowRequest highComplexityRequest = TestDataGenerator.generateHighComplexityRequest();
        List<LoadTestFramework.LoadTestResult> highComplexityResults = framework.runThreadPoolSizingTest(
            highComplexityRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(highComplexityResults);
        
        // Analyze results
        analyzeComplexityResults(simpleResults, mediumResults, complexResults, highComplexityResults);
    }
    
    /**
     * Run sustained load test
     */
    private static void runSustainedLoadTest(LoadTestFramework framework) {
        log.info("=== SUSTAINED LOAD TEST ===");
        
        CashFlowRequest request = TestDataGenerator.generateMediumRequest();
        
        // Test with optimal thread count (assuming 16 threads based on typical results)
        int optimalThreadCount = 16;
        int totalRequests = 500;
        int rampUpSeconds = 30;
        
        LoadTestFramework.LoadTestResult result = framework.runLoadTest(
            "SUSTAINED_LOAD", request, optimalThreadCount, totalRequests, rampUpSeconds);
        
        log.info("Sustained load test result: {}", result);
        
        // Check if system can handle sustained load
        if (result.getErrorCount() == 0 && result.getThroughputPerSecond() > 10) {
            log.info("✅ System handles sustained load well");
        } else {
            log.warn("⚠️ System may struggle with sustained load");
        }
    }
    
    /**
     * Run burst load test
     */
    private static void runBurstLoadTest(LoadTestFramework framework) {
        log.info("=== BURST LOAD TEST ===");
        
        CashFlowRequest request = TestDataGenerator.generateSimpleRequest();
        
        // Test with high thread count for burst scenario
        int burstThreadCount = 64;
        int totalRequests = 200;
        int rampUpSeconds = 5; // Quick ramp-up for burst
        
        LoadTestFramework.LoadTestResult result = framework.runLoadTest(
            "BURST_LOAD", request, burstThreadCount, totalRequests, rampUpSeconds);
        
        log.info("Burst load test result: {}", result);
        
        // Check if system can handle burst load
        if (result.getErrorCount() < (totalRequests * 0.05)) { // Less than 5% errors
            log.info("✅ System handles burst load well");
        } else {
            log.warn("⚠️ System struggles with burst load");
        }
    }
    
    /**
     * Analyze results across different complexity levels
     */
    private static void analyzeComplexityResults(List<LoadTestFramework.LoadTestResult> simpleResults,
                                               List<LoadTestFramework.LoadTestResult> mediumResults,
                                               List<LoadTestFramework.LoadTestResult> complexResults,
                                               List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        log.info("=== COMPLEXITY ANALYSIS ===");
        
        // Find optimal thread count for each complexity
        int simpleOptimal = findOptimalThreadCount(simpleResults);
        int mediumOptimal = findOptimalThreadCount(mediumResults);
        int complexOptimal = findOptimalThreadCount(complexResults);
        int highComplexityOptimal = findOptimalThreadCount(highComplexityResults);
        
        log.info("Optimal thread counts:");
        log.info("  Simple complexity: {} threads", simpleOptimal);
        log.info("  Medium complexity: {} threads", mediumOptimal);
        log.info("  Complex complexity: {} threads", complexOptimal);
        log.info("  High complexity: {} threads", highComplexityOptimal);
        
        // Calculate average optimal thread count
        int averageOptimal = (simpleOptimal + mediumOptimal + complexOptimal + highComplexityOptimal) / 4;
        log.info("Average optimal thread count: {} threads", averageOptimal);
        
        // Recommendations
        log.info("=== RECOMMENDATIONS ===");
        log.info("1. For simple requests: Use {} threads", simpleOptimal);
        log.info("2. For medium complexity: Use {} threads", mediumOptimal);
        log.info("3. For complex requests: Use {} threads", complexOptimal);
        log.info("4. For high complexity: Use {} threads", highComplexityOptimal);
        log.info("5. General recommendation: Use {} threads as default", averageOptimal);
        
        // Performance scaling analysis
        analyzePerformanceScaling(simpleResults, mediumResults, complexResults, highComplexityResults);
    }
    
    /**
     * Find optimal thread count based on throughput and error rate
     */
    private static int findOptimalThreadCount(List<LoadTestFramework.LoadTestResult> results) {
        return results.stream()
            .filter(r -> r.getErrorCount() == 0) // No errors
            .max((r1, r2) -> Double.compare(r1.getThroughputPerSecond(), r2.getThroughputPerSecond()))
            .map(LoadTestFramework.LoadTestResult::getThreadCount)
            .orElse(8); // Default fallback
    }
    
    /**
     * Analyze performance scaling characteristics
     */
    private static void analyzePerformanceScaling(List<LoadTestFramework.LoadTestResult> simpleResults,
                                                List<LoadTestFramework.LoadTestResult> mediumResults,
                                                List<LoadTestFramework.LoadTestResult> complexResults,
                                                List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        log.info("=== PERFORMANCE SCALING ANALYSIS ===");
        
        // Analyze throughput scaling
        analyzeThroughputScaling("Simple", simpleResults);
        analyzeThroughputScaling("Medium", mediumResults);
        analyzeThroughputScaling("Complex", complexResults);
        analyzeThroughputScaling("High Complexity", highComplexityResults);
        
        // Analyze response time scaling
        analyzeResponseTimeScaling("Simple", simpleResults);
        analyzeResponseTimeScaling("Medium", mediumResults);
        analyzeResponseTimeScaling("Complex", complexResults);
        analyzeResponseTimeScaling("High Complexity", highComplexityResults);
    }
    
    private static void analyzeThroughputScaling(String complexity, List<LoadTestFramework.LoadTestResult> results) {
        log.info("{} - Throughput scaling:", complexity);
        for (LoadTestFramework.LoadTestResult result : results) {
            if (result.getErrorCount() == 0) {
                log.info("  {} threads: {:.2f} req/sec", 
                    result.getThreadCount(), result.getThroughputPerSecond());
            }
        }
    }
    
    private static void analyzeResponseTimeScaling(String complexity, List<LoadTestFramework.LoadTestResult> results) {
        log.info("{} - Response time scaling:", complexity);
        for (LoadTestFramework.LoadTestResult result : results) {
            if (result.getErrorCount() == 0) {
                log.info("  {} threads: avg={}ms, min={}ms, max={}ms", 
                    result.getThreadCount(), result.getAvgResponseTimeMs(),
                    result.getMinResponseTimeMs(), result.getMaxResponseTimeMs());
            }
        }
    }
}
