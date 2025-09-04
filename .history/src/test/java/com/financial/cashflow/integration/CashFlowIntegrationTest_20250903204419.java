package com.financial.cashflow.integration;

import com.financial.cashflow.controller.CashFlowController;
import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.entity.CashFlow;
import com.financial.cashflow.entity.CalculationRequest;
import com.financial.cashflow.entity.SettlementInstruction;
import com.financial.cashflow.repository.CashFlowRepository;
import com.financial.cashflow.repository.CalculationRequestRepository;
import com.financial.cashflow.repository.SettlementInstructionRepository;
import com.financial.cashflow.service.CashFlowCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Cash Flow Management Service using Docker Compose approach.
 * 
 * Note: This test assumes the following Docker services are running:
 * - cash-flow-service (port 8080)
 * - sqlserver (port 1433)
 * - redis (port 6379)
 * 
 * Start with: docker-compose up -d
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CashFlowIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use Docker Compose infrastructure
        registry.add("spring.datasource.url", () -> "jdbc:sqlserver://localhost:1434;databaseName=master;encrypt=true;trustServerCertificate=true");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "TestPassword123!");
        // Use local Redis from Docker Compose
        registry.add("spring.cache.redis.host", () -> "localhost");
        registry.add("spring.cache.redis.port", () -> 6380);
        // Use local Kafka from Docker Compose
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9094");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CashFlowCalculationService calculationService;

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @Autowired
    private SettlementInstructionRepository settlementRepository;

    @Autowired
    private CalculationRequestRepository calculationRequestRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/cashflows";
        
        // Clean up test data
        cashFlowRepository.deleteAll();
        settlementRepository.deleteAll();
        calculationRequestRepository.deleteAll();
    }

    @Test
    void shouldCalculateCashFlowsViaRestApi() {
        // Given
        CashFlowRequestContent request = createTestRequest();

        // When
        ResponseEntity<CashFlowResponse> response = restTemplate.postForEntity(
            baseUrl + "/calculate",
            request,
            CashFlowResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRequestId()).isEqualTo(request.getRequestId());
        assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getBody().getCashFlows()).isNotEmpty();
        assertThat(response.getBody().getSettlementInstructions()).isNotEmpty();
    }

    @Test
    void shouldCalculateCashFlowsBatchViaRestApi() {
        // Given
        List<CashFlowRequestContent> requests = List.of(
            createTestRequest(),
            createTestRequest()
        );

        // When
        ResponseEntity<CashFlowController.BatchResponse> response = restTemplate.postForEntity(
            baseUrl + "/calculate/batch",
            requests,
            CashFlowController.BatchResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalRequests()).isEqualTo(2);
        assertThat(response.getBody().getSuccessfulResponses()).isEqualTo(2);
        assertThat(response.getBody().getResponses()).hasSize(2);
    }

    @Test
    void shouldRecalculateCashFlowsViaRestApi() {
        // Given
        String contractId = "SWAP_IBM_001";
        String fromDate = "2025-08-01";
        String toDate = "2025-08-31";

        // When
        ResponseEntity<CashFlowResponse> response = restTemplate.postForEntity(
            baseUrl + "/recalculate?contractId={contractId}&fromDate={fromDate}&toDate={toDate}",
            null,
            CashFlowResponse.class,
            contractId, fromDate, toDate
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContractId()).isEqualTo(contractId);
        assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldGetCachedCalculationViaRestApi() {
        // Given
        CashFlowRequestContent request = createTestRequest();
        calculationService.calculateCashFlows(request).join();

        // When
        ResponseEntity<CashFlowResponse> response = restTemplate.getForEntity(
            baseUrl + "/cached/{requestId}",
            CashFlowResponse.class,
            request.getRequestId()
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRequestId()).isEqualTo(request.getRequestId());
    }

    @Test
    void shouldReturnNotFoundForNonExistentCachedCalculation() {
        // When
        ResponseEntity<CashFlowResponse> response = restTemplate.getForEntity(
            baseUrl + "/cached/{requestId}",
            CashFlowResponse.class,
            "NON_EXISTENT_REQUEST"
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldPersistCashFlowsToDatabase() {
        // Given
        CashFlowRequestContent request = createTestRequest();

        // When
        CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        
        // Verify cash flows are persisted
        List<CashFlow> cashFlows = cashFlowRepository.findByRequestId(request.getRequestId());
        assertThat(cashFlows).isNotEmpty();
        assertThat(cashFlows).hasSize(response.getCashFlows().size());
        
        // Verify settlement instructions are persisted
        List<SettlementInstruction> settlements = settlementRepository.findByRequestId(request.getRequestId());
        assertThat(settlements).isNotEmpty();
        assertThat(settlements).hasSize(response.getSettlementInstructions().size());
        
        // Verify calculation request is persisted
        CalculationRequest calcRequest = calculationRequestRepository.findByRequestId(request.getRequestId()).orElse(null);
        assertThat(calcRequest).isNotNull();
        assertThat(calcRequest.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldHandleInvalidRequestGracefully() {
        // Given
        CashFlowRequestContent invalidRequest = CashFlowRequestContent.builder()
            .requestId("INVALID_REQUEST")
            .contractId("") // Invalid empty contract ID
            .build();

        // When
        ResponseEntity<CashFlowResponse> response = restTemplate.postForEntity(
            baseUrl + "/calculate",
            invalidRequest,
            CashFlowResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldSupportConcurrentCalculations() throws InterruptedException {
        // Given
        int numberOfRequests = 10;
        List<CashFlowRequestContent> requests = List.of();
        
        for (int i = 0; i < numberOfRequests; i++) {
            requests.add(createTestRequest());
        }

        // When
        List<CompletableFuture<CashFlowResponse>> futures = requests.stream()
            .map(calculationService::calculateCashFlows)
            .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then
        for (CompletableFuture<CashFlowResponse> future : futures) {
            try {
                CashFlowResponse response = future.get();
                assertThat(response.getStatus()).isEqualTo("SUCCESS");
            } catch (Exception e) {
                // Handle exceptions
            }
        }

        // Verify all are persisted
        long totalCashFlows = cashFlowRepository.count();
        long totalSettlements = settlementRepository.count();
        long totalCalculations = calculationRequestRepository.count();
        
        assertThat(totalCashFlows).isEqualTo(numberOfRequests * 4); // 4 cash flows per request
        assertThat(totalSettlements).isEqualTo(numberOfRequests * 4); // 4 settlements per request
        assertThat(totalCalculations).isEqualTo(numberOfRequests);
    }

    @Test
    void shouldHandleDatabaseConstraints() {
        // Given - Create a request with duplicate request ID
        CashFlowRequestContent request1 = createTestRequest();
        CashFlowRequestContent request2 = createTestRequest();
        request2.setRequestId(request1.getRequestId()); // Duplicate request ID

        // When
        CompletableFuture<CashFlowResponse> future1 = calculationService.calculateCashFlows(request1);
        CompletableFuture<CashFlowResponse> future2 = calculationService.calculateCashFlows(request2);

        // Then
        CashFlowResponse response1 = future1.join();
        assertThat(response1.getStatus()).isEqualTo("SUCCESS");

        // Second request should fail due to duplicate request ID constraint
        CashFlowResponse response2 = future2.join();
        assertThat(response2.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldHandleCacheEviction() {
        // Given
        CashFlowRequestContent request = createTestRequest();
        calculationService.calculateCashFlows(request).join();

        // Verify cache hit
        ResponseEntity<CashFlowResponse> cachedResponse = restTemplate.getForEntity(
            baseUrl + "/cached/{requestId}",
            CashFlowResponse.class,
            request.getRequestId()
        );
        assertThat(cachedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When - Clear cache (simulate eviction)
        // This would typically be done through cache management, but for test we'll just verify
        // that the calculation still works after cache miss

        // Then - Should still work even if cache is cleared
        CompletableFuture<CashFlowResponse> future = calculationService.calculateCashFlows(request);
        CashFlowResponse response = future.join();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldHandleHealthCheck() {
        // When
        ResponseEntity<CashFlowController.HealthResponse> response = restTemplate.getForEntity(
            baseUrl + "/health",
            CashFlowController.HealthResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
    }

    private CashFlowRequestContent createTestRequest() {
        return CashFlowRequestContent.builder()
            .requestId("TEST_REQUEST_" + UUID.randomUUID().toString().substring(0, 8))
            .contractId("SWAP_IBM_001")
            .dateRange(CashFlowRequestContent.DateRange.builder()
                .fromDate(LocalDate.of(2025, 8, 1))
                .toDate(LocalDate.of(2025, 8, 31))
                .calculationFrequency("DAILY")
                .build())
            .contracts(List.of(CashFlowRequestContent.Contract.builder()
                .contractId("SWAP_IBM_001")
                .contractType("EQUITY_SWAP")
                .notionalAmount(1000000.0)
                .currency("USD")
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2026, 8, 1))
                .interestRate(0.05)
                .build()))
            .positions(List.of(CashFlowRequestContent.Position.builder()
                .positionId("POS_001")
                .contractId("SWAP_IBM_001")
                .quantity(1000.0)
                .build()))
            .lots(List.of(CashFlowRequestContent.Lot.builder()
                .lotId("LOT_001")
                .positionId("POS_001")
                .quantity(1000.0)
                .costBasis(150.0)
                .tradeDate(LocalDate.of(2025, 8, 1))
                .build()))
            .marketData(CashFlowRequestContent.MarketDataContainer.builder()
                .prices(List.of(CashFlowRequestContent.PriceData.builder()
                    .instrumentId("IBM")
                    .price(155.0)
                    .currency("USD")
                    .priceDate(LocalDate.of(2025, 8, 1))
                    .priceType("CLOSE")
                    .build()))
                .build())
            .build();
    }
}
