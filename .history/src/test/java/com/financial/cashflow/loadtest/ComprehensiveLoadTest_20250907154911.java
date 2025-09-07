package com.financial.cashflow.loadtest;

import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive Load Test for Thread Pool Sizing Analysis
 * Tests different complexity levels and thread counts to determine optimal configuration
 */
@Slf4j
public class ComprehensiveLoadTest {
    
    private static final String BASE_URL = "http://localhost:8080";
    
    public static void main(String[] args) {
        log.info("Starting Comprehensive Load Test for Thread Pool Sizing");
        
        LoadTestFramework framework = new LoadTestFramework(BASE_URL);
        
        // Test different thread pool sizes
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};
        int requestsPerThreadCount = 20;
        int rampUpSeconds = 5;
        
        // Test 1: Simple complexity (1 contract, 1 position, 1 lot)
        log.info("=== TEST 1: SIMPLE COMPLEXITY ===");
        CashFlowRequest simpleRequest = TestDataGenerator.generateSimpleRequest();
        List<LoadTestFramework.LoadTestResult> simpleResults = framework.runThreadPoolSizingTest(
            simpleRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(simpleResults);
        
        // Test 2: Medium complexity (2 contracts, 2 positions each, 3 lots each)
        log.info("=== TEST 2: MEDIUM COMPLEXITY ===");
        CashFlowRequest mediumRequest = TestDataGenerator.generateMediumRequest();
        List<LoadTestFramework.LoadTestResult> mediumResults = framework.runThreadPoolSizingTest(
            mediumRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(mediumResults);
        
        // Test 3: Complex complexity (3 contracts, 3 positions each, 5 lots each)
        log.info("=== TEST 3: COMPLEX COMPLEXITY ===");
        CashFlowRequest complexRequest = TestDataGenerator.generateComplexRequest();
        List<LoadTestFramework.LoadTestResult> complexResults = framework.runThreadPoolSizingTest(
            complexRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(complexResults);
        
        // Test 4: High complexity (5 contracts, 4 positions each, 8 lots each)
        log.info("=== TEST 4: HIGH COMPLEXITY ===");
        CashFlowRequest highComplexityRequest = TestDataGenerator.generateHighComplexityRequest();
        List<LoadTestFramework.LoadTestResult> highComplexityResults = framework.runThreadPoolSizingTest(
            highComplexityRequest, threadCounts, requestsPerThreadCount, rampUpSeconds);
        LoadTestFramework.printResultsTable(highComplexityResults);
        
        // Analysis and recommendations
        analyzeResults(simpleResults, mediumResults, complexResults, highComplexityResults);
        
        // Performance scaling analysis
        analyzePerformanceScaling(simpleResults, mediumResults, complexResults, highComplexityResults);
        
        log.info("Comprehensive load test completed");
    }
    
    /**
     * Analyze results and provide recommendations
     */
    private static void analyzeResults(List<LoadTestFramework.LoadTestResult> simpleResults,
                                     List<LoadTestFramework.LoadTestResult> mediumResults,
                                     List<LoadTestFramework.LoadTestResult> complexResults,
                                     List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        log.info("=== THREAD POOL SIZING ANALYSIS ===");
        
        // Find optimal thread count for each complexity
        int simpleOptimal = findOptimalThreadCount(simpleResults);
        int mediumOptimal = findOptimalThreadCount(mediumResults);
        int complexOptimal = findOptimalThreadCount(complexResults);
        int highComplexityOptimal = findOptimalThreadCount(highComplexityResults);
        
        log.info("Optimal thread counts by complexity:");
        log.info("  Simple (1C-1P-1L): {} threads", simpleOptimal);
        log.info("  Medium (2C-2P-3L): {} threads", mediumOptimal);
        log.info("  Complex (3C-3P-5L): {} threads", complexOptimal);
        log.info("  High (5C-4P-8L): {} threads", highComplexityOptimal);
        
        // Calculate weighted average (more weight to complex scenarios)
        int weightedAverage = (simpleOptimal + mediumOptimal * 2 + complexOptimal * 3 + highComplexityOptimal * 4) / 10;
        log.info("Weighted average optimal thread count: {} threads", weightedAverage);
        
        // Find the sweet spot (best performance across all complexities)
        int sweetSpot = findSweetSpot(simpleResults, mediumResults, complexResults, highComplexityResults);
        log.info("Sweet spot thread count (best overall): {} threads", sweetSpot);
        
        // Recommendations
        log.info("=== RECOMMENDATIONS ===");
        log.info("1. Default thread pool size: {} threads", sweetSpot);
        log.info("2. For simple requests: {} threads", simpleOptimal);
        log.info("3. For medium complexity: {} threads", mediumOptimal);
        log.info("4. For complex requests: {} threads", complexOptimal);
        log.info("5. For high complexity: {} threads", highComplexityOptimal);
        log.info("6. Maximum recommended: {} threads", Math.max(sweetSpot, highComplexityOptimal));
        
        // Performance characteristics
        analyzePerformanceCharacteristics(simpleResults, mediumResults, complexResults, highComplexityResults);
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
     * Find sweet spot thread count that performs well across all complexities
     */
    private static int findSweetSpot(List<LoadTestFramework.LoadTestResult> simpleResults,
                                   List<LoadTestFramework.LoadTestResult> mediumResults,
                                   List<LoadTestFramework.LoadTestResult> complexResults,
                                   List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};
        double bestScore = 0;
        int bestThreadCount = 8;
        
        for (int threadCount : threadCounts) {
            double score = calculatePerformanceScore(threadCount, simpleResults, mediumResults, complexResults, highComplexityResults);
            if (score > bestScore) {
                bestScore = score;
                bestThreadCount = threadCount;
            }
        }
        
        return bestThreadCount;
    }
    
    /**
     * Calculate performance score for a given thread count across all complexities
     */
    private static double calculatePerformanceScore(int threadCount,
                                                  List<LoadTestFramework.LoadTestResult> simpleResults,
                                                  List<LoadTestFramework.LoadTestResult> mediumResults,
                                                  List<LoadTestFramework.LoadTestResult> complexResults,
                                                  List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        double score = 0;
        
        // Simple complexity (weight: 1)
        score += getThroughputForThreadCount(threadCount, simpleResults) * 1;
        
        // Medium complexity (weight: 2)
        score += getThroughputForThreadCount(threadCount, mediumResults) * 2;
        
        // Complex complexity (weight: 3)
        score += getThroughputForThreadCount(threadCount, complexResults) * 3;
        
        // High complexity (weight: 4)
        score += getThroughputForThreadCount(threadCount, highComplexityResults) * 4;
        
        return score;
    }
    
    /**
     * Get throughput for a specific thread count
     */
    private static double getThroughputForThreadCount(int threadCount, List<LoadTestFramework.LoadTestResult> results) {
        return results.stream()
            .filter(r -> r.getThreadCount() == threadCount && r.getErrorCount() == 0)
            .mapToDouble(LoadTestFramework.LoadTestResult::getThroughputPerSecond)
            .findFirst()
            .orElse(0.0);
    }
    
    /**
     * Analyze performance characteristics
     */
    private static void analyzePerformanceCharacteristics(List<LoadTestFramework.LoadTestResult> simpleResults,
                                                        List<LoadTestFramework.LoadTestResult> mediumResults,
                                                        List<LoadTestFramework.LoadTestResult> complexResults,
                                                        List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        log.info("=== PERFORMANCE CHARACTERISTICS ===");
        
        // Find where performance starts to degrade
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};
        
        for (int threadCount : threadCounts) {
            double simpleThroughput = getThroughputForThreadCount(threadCount, simpleResults);
            double mediumThroughput = getThroughputForThreadCount(threadCount, mediumResults);
            double complexThroughput = getThroughputForThreadCount(threadCount, complexResults);
            double highComplexityThroughput = getThroughputForThreadCount(threadCount, highComplexityResults);
            
            log.info("{} threads: Simple={:.2f}, Medium={:.2f}, Complex={:.2f}, High={:.2f} req/sec",
                threadCount, simpleThroughput, mediumThroughput, complexThroughput, highComplexityThroughput);
        }
        
        // Find performance degradation point
        int degradationPoint = findPerformanceDegradationPoint(simpleResults, mediumResults, complexResults, highComplexityResults);
        if (degradationPoint > 0) {
            log.info("Performance degradation starts at: {} threads", degradationPoint);
        } else {
            log.info("No significant performance degradation observed in tested range");
        }
    }
    
    /**
     * Find where performance starts to degrade
     */
    private static int findPerformanceDegradationPoint(List<LoadTestFramework.LoadTestResult> simpleResults,
                                                     List<LoadTestFramework.LoadTestResult> mediumResults,
                                                     List<LoadTestFramework.LoadTestResult> complexResults,
                                                     List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};
        double maxScore = 0;
        int degradationPoint = 0;
        
        for (int i = 0; i < threadCounts.length; i++) {
            double score = calculatePerformanceScore(threadCounts[i], simpleResults, mediumResults, complexResults, highComplexityResults);
            if (score > maxScore) {
                maxScore = score;
            } else if (score < maxScore * 0.9) { // 10% degradation
                degradationPoint = threadCounts[i];
                break;
            }
        }
        
        return degradationPoint;
    }
    
    /**
     * Analyze performance scaling characteristics
     */
    private static void analyzePerformanceScaling(List<LoadTestFramework.LoadTestResult> simpleResults,
                                                List<LoadTestFramework.LoadTestResult> mediumResults,
                                                List<LoadTestFramework.LoadTestResult> complexResults,
                                                List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        log.info("=== PERFORMANCE SCALING ANALYSIS ===");
        
        // Calculate scaling efficiency
        calculateScalingEfficiency("Simple", simpleResults);
        calculateScalingEfficiency("Medium", mediumResults);
        calculateScalingEfficiency("Complex", complexResults);
        calculateScalingEfficiency("High Complexity", highComplexityResults);
        
        // Response time analysis
        analyzeResponseTimeScaling(simpleResults, mediumResults, complexResults, highComplexityResults);
    }
    
    private static void calculateScalingEfficiency(String complexity, List<LoadTestFramework.LoadTestResult> results) {
        log.info("{} - Scaling efficiency:", complexity);
        
        double baselineThroughput = getThroughputForThreadCount(1, results);
        if (baselineThroughput == 0) return;
        
        for (LoadTestFramework.LoadTestResult result : results) {
            if (result.getErrorCount() == 0) {
                double efficiency = (result.getThroughputPerSecond() / baselineThroughput) / result.getThreadCount() * 100;
                log.info("  {} threads: {:.2f} req/sec, {:.1f}% efficiency",
                    result.getThreadCount(), result.getThroughputPerSecond(), efficiency);
            }
        }
    }
    
    private static void analyzeResponseTimeScaling(List<LoadTestFramework.LoadTestResult> simpleResults,
                                                 List<LoadTestFramework.LoadTestResult> mediumResults,
                                                 List<LoadTestFramework.LoadTestResult> complexResults,
                                                 List<LoadTestFramework.LoadTestResult> highComplexityResults) {
        log.info("=== RESPONSE TIME SCALING ===");
        
        analyzeResponseTimeForComplexity("Simple", simpleResults);
        analyzeResponseTimeForComplexity("Medium", mediumResults);
        analyzeResponseTimeForComplexity("Complex", complexResults);
        analyzeResponseTimeForComplexity("High Complexity", highComplexityResults);
    }
    
    private static void analyzeResponseTimeForComplexity(String complexity, List<LoadTestFramework.LoadTestResult> results) {
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
