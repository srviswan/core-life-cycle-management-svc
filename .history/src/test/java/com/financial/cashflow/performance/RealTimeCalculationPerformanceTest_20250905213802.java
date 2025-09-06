package com.financial.cashflow.performance;

import com.financial.cashflow.fixtures.TestDataFixtures;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.service.CashFlowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for real-time cash flow calculations
 * Tests speed, throughput, and scalability of real-time calculations
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Real-Time Calculation Performance Tests")
class RealTimeCalculationPerformanceTest {

    @Autowired
    private CashFlowService cashFlowService;

    @Autowired
    private TestDataFixtures testDataFixtures;

    @Test
    @DisplayName("Should complete single real-time calculation within performance threshold")
    void testSingleRealTimeCalculationPerformance() {
        // Arrange
        CashFlowRequest request = testDataFixtures.createIbmEquitySwapRequest();
        
        // Act
        long startTime = System.currentTimeMillis();
        var response = cashFlowService.calculate(request);
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(processingTime < 1000, 
                "Single real-time calculation should complete within 1 second, took: " + processingTime + "ms");
        
        // Verify processing time metadata
        assertNotNull(response.getMetadata());
        assertNotNull(response.getMetadata().getProcessingTimeMs());
        assertTrue(response.getMetadata().getProcessingTimeMs() < 1000);
    }

    @Test
    @DisplayName("Should handle multiple sequential calculations efficiently")
    void testSequentialCalculationsPerformance() {
        // Arrange
        List<CashFlowRequest> requests = List.of(
                testDataFixtures.createIbmEquitySwapRequest(),
                testDataFixtures.createLargeNotionalEquitySwapRequest(),
                testDataFixtures.createHighVolatilityRequest(),
                testDataFixtures.createLowVolatilityRequest(),
                testDataFixtures.createDividendPayingRequest()
        );

        // Act
        long startTime = System.currentTimeMillis();
        List<Long> individualTimes = new ArrayList<>();
        
        for (CashFlowRequest request : requests) {
            long requestStart = System.currentTimeMillis();
            var response = cashFlowService.calculate(request);
            long requestEnd = System.currentTimeMillis();
            
            individualTimes.add(requestEnd - requestStart);
            assertEquals("SUCCESS", response.getStatus());
        }
        
        long totalTime = System.currentTimeMillis() - startTime;

        // Assert
        assertTrue(totalTime < 5000, 
                "5 sequential calculations should complete within 5 seconds, took: " + totalTime + "ms");
        
        // Each individual calculation should be fast
        for (int i = 0; i < individualTimes.size(); i++) {
            assertTrue(individualTimes.get(i) < 1000, 
                    "Calculation " + (i + 1) + " should complete within 1 second, took: " + individualTimes.get(i) + "ms");
        }
    }

    @Test
    @DisplayName("Should handle concurrent calculations efficiently")
    void testConcurrentCalculationsPerformance() throws Exception {
        // Arrange
        int numberOfConcurrentRequests = 10;
        List<CashFlowRequest> requests = new ArrayList<>();
        
        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            CashFlowRequest request = testDataFixtures.createIbmEquitySwapRequest();
            request.setRequestId("CONCURRENT_" + i);
            request.getContracts().get(0).setContractId("CONCURRENT_CONTRACT_" + i);
            requests.add(request);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        // Act
        long startTime = System.currentTimeMillis();
        
        for (CashFlowRequest request : requests) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long requestStart = System.currentTimeMillis();
                var response = cashFlowService.calculate(request);
                long requestEnd = System.currentTimeMillis();
                
                assertEquals("SUCCESS", response.getStatus());
                return requestEnd - requestStart;
            }, executor);
            
            futures.add(future);
        }

        // Wait for all calculations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        
        allFutures.get(10, TimeUnit.SECONDS); // Timeout after 10 seconds
        
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        // Assert
        assertTrue(totalTime < 10000, 
                "10 concurrent calculations should complete within 10 seconds, took: " + totalTime + "ms");
        
        // Check individual calculation times
        for (int i = 0; i < futures.size(); i++) {
            long individualTime = futures.get(i).get();
            assertTrue(individualTime < 2000, 
                    "Concurrent calculation " + (i + 1) + " should complete within 2 seconds, took: " + individualTime + "ms");
        }
    }

    @Test
    @DisplayName("Should handle large notional amounts efficiently")
    void testLargeNotionalPerformance() {
        // Arrange
        CashFlowRequest request = testDataFixtures.createLargeNotionalEquitySwapRequest();
        
        // Act
        long startTime = System.currentTimeMillis();
        var response = cashFlowService.calculate(request);
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(processingTime < 2000, 
                "Large notional calculation should complete within 2 seconds, took: " + processingTime + "ms");
        
        // Verify the calculation results are reasonable for large notional
        assertNotNull(response.getContractResults());
        assertFalse(response.getContractResults().isEmpty());
        assertNotNull(response.getContractResults().get(0).getTotalCashFlows());
        assertTrue(response.getContractResults().get(0).getTotalCashFlows().doubleValue() > 0);
    }

    @Test
    @DisplayName("Should handle multiple contracts efficiently")
    void testMultipleContractsPerformance() {
        // Arrange
        CashFlowRequest request = testDataFixtures.createMultiUnderlyingRequest();
        
        // Act
        long startTime = System.currentTimeMillis();
        var response = cashFlowService.calculate(request);
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(processingTime < 2000, 
                "Multiple contracts calculation should complete within 2 seconds, took: " + processingTime + "ms");
        
        // Verify all contracts were processed
        assertEquals(3, response.getContractResults().size());
        assertEquals(3, response.getSummary().getTotalContracts());
        
        // All contracts should have successful results
        for (var contractResult : response.getContractResults()) {
            assertEquals("SUCCESS", contractResult.getStatus());
            assertNotNull(contractResult.getTotalCashFlows());
        }
    }

    @Test
    @DisplayName("Should handle mixed contract types efficiently")
    void testMixedContractTypesPerformance() {
        // Arrange
        CashFlowRequest request = testDataFixtures.createMixedContractTypesRequest();
        
        // Act
        long startTime = System.currentTimeMillis();
        var response = cashFlowService.calculate(request);
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(processingTime < 2000, 
                "Mixed contract types calculation should complete within 2 seconds, took: " + processingTime + "ms");
        
        // Verify all contract types were processed
        assertEquals(3, response.getContractResults().size());
        
        // Check that different contract types produce different results
        var results = response.getContractResults();
        assertNotEquals(results.get(0).getTotalCashFlows(), results.get(1).getTotalCashFlows());
        assertNotEquals(results.get(1).getTotalCashFlows(), results.get(2).getTotalCashFlows());
    }

    @Test
    @DisplayName("Should handle different market data strategies efficiently")
    void testDifferentMarketDataStrategiesPerformance() {
        // Test SELF_CONTAINED strategy
        CashFlowRequest selfContainedRequest = testDataFixtures.createIbmEquitySwapRequest();
        
        long startTime = System.currentTimeMillis();
        var selfContainedResponse = cashFlowService.calculate(selfContainedRequest);
        long selfContainedTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(selfContainedResponse);
        assertEquals("SUCCESS", selfContainedResponse.getStatus());
        assertTrue(selfContainedTime < 1000, 
                "Self-contained market data should complete within 1 second, took: " + selfContainedTime + "ms");

        // Test ENDPOINTS strategy
        CashFlowRequest endpointsRequest = testDataFixtures.createEndpointsMarketDataRequest();
        
        startTime = System.currentTimeMillis();
        var endpointsResponse = cashFlowService.calculate(endpointsRequest);
        long endpointsTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(endpointsResponse);
        assertEquals("SUCCESS", endpointsResponse.getStatus());
        assertTrue(endpointsTime < 2000, 
                "Endpoints market data should complete within 2 seconds, took: " + endpointsTime + "ms");
    }

    @Test
    @DisplayName("Should handle different currencies efficiently")
    void testDifferentCurrenciesPerformance() {
        // Test USD
        CashFlowRequest usdRequest = testDataFixtures.createIbmEquitySwapRequest();
        
        long startTime = System.currentTimeMillis();
        var usdResponse = cashFlowService.calculate(usdRequest);
        long usdTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(usdResponse);
        assertEquals("SUCCESS", usdResponse.getStatus());
        assertTrue(usdTime < 1000, 
                "USD calculation should complete within 1 second, took: " + usdTime + "ms");

        // Test EUR
        CashFlowRequest eurRequest = testDataFixtures.createEurBasedRequest();
        
        startTime = System.currentTimeMillis();
        var eurResponse = cashFlowService.calculate(eurRequest);
        long eurTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(eurResponse);
        assertEquals("SUCCESS", eurResponse.getStatus());
        assertTrue(eurTime < 1000, 
                "EUR calculation should complete within 1 second, took: " + eurTime + "ms");
    }

    @Test
    @DisplayName("Should handle edge cases efficiently")
    void testEdgeCasesPerformance() {
        // Test zero notional
        CashFlowRequest zeroNotionalRequest = testDataFixtures.createZeroNotionalRequest();
        
        long startTime = System.currentTimeMillis();
        var zeroResponse = cashFlowService.calculate(zeroNotionalRequest);
        long zeroTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(zeroResponse);
        assertEquals("SUCCESS", zeroResponse.getStatus());
        assertTrue(zeroTime < 1000, 
                "Zero notional calculation should complete within 1 second, took: " + zeroTime + "ms");

        // Test null notional (should use default)
        CashFlowRequest nullNotionalRequest = testDataFixtures.createNullNotionalRequest();
        
        startTime = System.currentTimeMillis();
        var nullResponse = cashFlowService.calculate(nullNotionalRequest);
        long nullTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(nullResponse);
        assertEquals("SUCCESS", nullResponse.getStatus());
        assertTrue(nullTime < 1000, 
                "Null notional calculation should complete within 1 second, took: " + nullTime + "ms");
    }

    @Test
    @DisplayName("Should maintain consistent performance across multiple runs")
    void testPerformanceConsistency() {
        // Arrange
        CashFlowRequest request = testDataFixtures.createIbmEquitySwapRequest();
        List<Long> executionTimes = new ArrayList<>();
        int numberOfRuns = 5;

        // Act
        for (int i = 0; i < numberOfRuns; i++) {
            long startTime = System.currentTimeMillis();
            var response = cashFlowService.calculate(request);
            long endTime = System.currentTimeMillis();
            
            executionTimes.add(endTime - startTime);
            assertEquals("SUCCESS", response.getStatus());
        }

        // Assert
        // Calculate statistics
        long totalTime = executionTimes.stream().mapToLong(Long::longValue).sum();
        double averageTime = totalTime / (double) numberOfRuns;
        long maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        // Performance should be consistent
        assertTrue(averageTime < 1000, 
                "Average execution time should be under 1 second, was: " + averageTime + "ms");
        assertTrue(maxTime < 1500, 
                "Maximum execution time should be under 1.5 seconds, was: " + maxTime + "ms");
        assertTrue(minTime > 0, 
                "Minimum execution time should be positive, was: " + minTime + "ms");
        
        // Variation should not be too high (max should not be more than 2x min)
        assertTrue(maxTime <= minTime * 2, 
                "Performance variation should be reasonable, max: " + maxTime + "ms, min: " + minTime + "ms");
    }
}
