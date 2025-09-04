package com.financial.cashflow.performance;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.CashFlowCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for Cash Flow Management Service.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CashFlowPerformanceTest {

    @Container
    static MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(
        DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
        .acceptLicense()
        .withPassword("TestPassword123!")
        .withInitScript("init-test-db.sql");

    @Container
    static RedisContainer<?> redis = new RedisContainer<>(
        DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
        registry.add("spring.cache.redis.host", redis::getHost);
        registry.add("spring.cache.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CashFlowCalculationService calculationService;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        // Create thread pool for concurrent testing
        executorService = Executors.newFixedThreadPool(20);
    }

    @Test
    void shouldHandleSingleCalculationUnder100ms() {
        // Given
        CashFlowRequestContent request = createTestRequest();

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
        CashFlowResponse response = future.join();
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(100); // Should complete under 100ms
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getCashFlows()).isNotEmpty();
        assertThat(response.getSettlementInstructions()).isNotEmpty();
    }

    @Test
    void shouldHandleConcurrentCalculations() throws InterruptedException {
        // Given
        int numberOfRequests = 100;
        List<CashFlowRequestContent> requests = new ArrayList<>();
        
        for (int i = 0; i < numberOfRequests; i++) {
            requests.add(createTestRequest());
        }

        // When
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<CashFlowResponse>> futures = new ArrayList<>();
        
        for (CashFlowRequestContent request : requests) {
            CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
            futures.add(future);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(5000); // Should complete under 5 seconds
        
        for (CompletableFuture<CashFlowResponse> future : futures) {
            CashFlowResponse response = future.get();
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }
    }

    @Test
    void shouldHandleHighConcurrencyWithThreadPool() throws InterruptedException {
        // Given
        int numberOfRequests = 200;
        List<CompletableFuture<CashFlowResponse>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfRequests; i++) {
            CashFlowRequestContent request = createTestRequest();
            CompletableFuture<CashFlowResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return calculationService.calculateCashFlows(request).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);
            futures.add(future);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(10000); // Should complete under 10 seconds
        
        int successCount = 0;
        for (CompletableFuture<CashFlowResponse> future : futures) {
            try {
                CashFlowResponse response = future.get();
                if ("SUCCESS".equals(response.getStatus())) {
                    successCount++;
                }
            } catch (Exception e) {
                // Count failures
            }
        }
        
        // At least 95% should succeed
        double successRate = (double) successCount / numberOfRequests;
        assertThat(successRate).isGreaterThan(0.95);
    }

    @Test
    void shouldHandleBatchCalculationsEfficiently() {
        // Given
        int batchSize = 50;
        List<CashFlowRequestContent> requests = new ArrayList<>();
        
        for (int i = 0; i < batchSize; i++) {
            requests.add(createTestRequest());
        }

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<CashFlowResponse>> future = calculationService.calculateCashFlowsBatch(requests);
        List<CashFlowResponse> responses = future.join();
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(3000); // Should complete under 3 seconds
        assertThat(responses).hasSize(batchSize);
        
        for (CashFlowResponse response : responses) {
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }
    }

    @Test
    void shouldHandleCachePerformance() {
        // Given
        CashFlowRequestContent request = createTestRequest();

        // When - First calculation (cache miss)
        long startTime = System.currentTimeMillis();
        CompletableFuture<CashFlowResponse> firstFuture = calculationService.calculateCashFlows(request);
        CashFlowResponse firstResponse = firstFuture.join();
        long firstDuration = System.currentTimeMillis() - startTime;

        // Second calculation (cache hit)
        startTime = System.currentTimeMillis();
        CompletableFuture<CashFlowResponse> secondFuture = calculationService.calculateCashFlows(request);
        CashFlowResponse secondResponse = secondFuture.join();
        long secondDuration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(firstResponse.getStatus()).isEqualTo("SUCCESS");
        assertThat(secondResponse.getStatus()).isEqualTo("SUCCESS");
        assertThat(secondDuration).isLessThan(firstDuration); // Cache hit should be faster
        assertThat(secondDuration).isLessThan(50); // Cache hit should be under 50ms
    }

    @Test
    void shouldHandleMemoryUsageUnderLoad() {
        // Given
        int numberOfRequests = 1000;
        List<CompletableFuture<CashFlowResponse>> futures = new ArrayList<>();

        // When
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < numberOfRequests; i++) {
            CashFlowRequestContent request = createTestRequest();
            CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
            futures.add(future);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Force garbage collection to get accurate memory measurement
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;

        // Then
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // Should use less than 100MB additional memory
        
        int successCount = 0;
        for (CompletableFuture<CashFlowResponse> future : futures) {
            try {
                CashFlowResponse response = future.get();
                if ("SUCCESS".equals(response.getStatus())) {
                    successCount++;
                }
            } catch (Exception e) {
                // Count failures
            }
        }
        
        // At least 90% should succeed under memory pressure
        double successRate = (double) successCount / numberOfRequests;
        assertThat(successRate).isGreaterThan(0.90);
    }

    @Test
    void shouldHandlePeakLoadSimulation() throws InterruptedException {
        // Given - Simulate peak load (4 PM scenario)
        int peakRequests = 500;
        List<CompletableFuture<CashFlowResponse>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        // Submit requests in bursts to simulate real-world load
        for (int burst = 0; burst < 5; burst++) {
            List<CompletableFuture<CashFlowResponse>> burstFutures = new ArrayList<>();
            
            for (int i = 0; i < peakRequests / 5; i++) {
                CashFlowRequestContent request = createTestRequest();
                CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
                burstFutures.add(future);
            }
            
            futures.addAll(burstFutures);
            
            // Small delay between bursts
            Thread.sleep(100);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(15000); // Should complete under 15 seconds
        
        int successCount = 0;
        for (CompletableFuture<CashFlowResponse> future : futures) {
            try {
                CashFlowResponse response = future.get();
                if ("SUCCESS".equals(response.getStatus())) {
                    successCount++;
                }
            } catch (Exception e) {
                // Count failures
            }
        }
        
        // At least 95% should succeed under peak load
        double successRate = (double) successCount / peakRequests;
        assertThat(successRate).isGreaterThan(0.95);
    }

    @Test
    void shouldHandleLongRunningCalculations() {
        // Given - Create a request with large date range
        CashFlowRequestContent request = createTestRequest();
        request.setDateRange(CashFlowRequestContent.DateRange.builder()
            .fromDate(LocalDate.of(2024, 1, 1))
            .toDate(LocalDate.of(2025, 12, 31))
            .calculationFrequency("DAILY")
            .build());

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
        CashFlowResponse response = future.join();
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(30000); // Should complete under 30 seconds
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getCashFlows()).isNotEmpty();
    }

    private CashFlowRequestContent createTestRequest() {
        return CashFlowRequestContent.builder()
            .requestId("PERF_TEST_" + UUID.randomUUID().toString().substring(0, 8))
            .contractId("SWAP_IBM_001")
            .dateRange(CashFlowRequestContent.DateRange.builder()
                .fromDate(LocalDate.of(2025, 8, 1))
                .toDate(LocalDate.of(2025, 8, 31))
                .calculationFrequency("DAILY")
                .build())
            .contract(CashFlowRequestContent.Contract.builder()
                .contractId("SWAP_IBM_001")
                .contractType("EQUITY_SWAP")
                .notionalAmount(new BigDecimal("1000000.00"))
                .currency("USD")
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2026, 8, 1))
                .interestRate(new BigDecimal("0.05"))
                .build())
            .positions(List.of(CashFlowRequestContent.Position.builder()
                .positionId("POS_001")
                .contractId("SWAP_IBM_001")
                .quantity(new BigDecimal("1000"))
                .build()))
            .lots(List.of(CashFlowRequestContent.Lot.builder()
                .lotId("LOT_001")
                .positionId("POS_001")
                .quantity(new BigDecimal("1000"))
                .costBasis(new BigDecimal("150.00"))
                .tradeDate(LocalDate.of(2025, 8, 1))
                .build()))
            .marketData(List.of(CashFlowRequestContent.MarketData.builder()
                .symbol("IBM")
                .date(LocalDate.of(2025, 8, 1))
                .closePrice(new BigDecimal("155.00"))
                .currency("USD")
                .build()))
            .build();
    }
}
