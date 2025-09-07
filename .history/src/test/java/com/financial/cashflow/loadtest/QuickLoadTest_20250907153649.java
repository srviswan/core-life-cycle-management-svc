package com.financial.cashflow.loadtest;

import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Quick Load Test to verify setup and get initial performance baseline
 */
@Slf4j
public class QuickLoadTest {
    
    private static final String BASE_URL = "http://localhost:8080";
    
    public static void main(String[] args) {
        log.info("Starting Quick Load Test");
        
        LoadTestFramework framework = new LoadTestFramework(BASE_URL);
        
        // Test with simple request and different thread counts
        int[] threadCounts = {1, 2, 4, 8, 16};
        int requestsPerThreadCount = 10;
        int rampUpSeconds = 2;
        
        CashFlowRequest request = TestDataGenerator.generateSimpleRequest();
        
        log.info("Testing with simple request: {} contracts, {} positions, {} lots", 
            1, 1, 1);
        
        var results = framework.runThreadPoolSizingTest(
            request, threadCounts, requestsPerThreadCount, rampUpSeconds);
        
        LoadTestFramework.printResultsTable(results);
        
        // Quick analysis
        var optimal = results.stream()
            .filter(r -> r.getErrorCount() == 0)
            .max((r1, r2) -> Double.compare(r1.getThroughputPerSecond(), r2.getThroughputPerSecond()))
            .orElse(null);
        
        if (optimal != null) {
            log.info("Quick test optimal thread count: {} threads", optimal.getThreadCount());
            log.info("Quick test throughput: {:.2f} req/sec", optimal.getThroughputPerSecond());
        } else {
            log.warn("No successful tests found");
        }
        
        log.info("Quick load test completed");
    }
}
