package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.impl.CashFlowEngineImpl;
import com.financial.cashflow.service.impl.CashFlowValidatorImpl;
import com.financial.cashflow.service.impl.EventPublisherImpl;
import com.financial.cashflow.service.impl.SettlementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CashFlowCalculationService.
 * Tests the service layer with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class CashFlowCalculationServiceTest {

    @Autowired
    private CashFlowCalculationService service;

    @MockBean
    private CashFlowValidator validator;

    @MockBean
    private CashFlowEngine engine;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(validator, engine, settlementService, eventPublisher, cacheService);
    }

    @Test
    @DisplayName("Should calculate cash flows successfully for valid request")
    void shouldCalculateCashFlowsSuccessfully() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        List<CashFlowResponse.CashFlow> expectedCashFlows = createSampleCashFlows();
        List<CashFlowResponse.SettlementInstruction> expectedSettlements = createSampleSettlements();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(expectedCashFlows);
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(expectedSettlements);
        when(cacheService.put(anyString(), any())).thenReturn(true);

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals(request.getRequestId(), response.getRequestId());
        assertEquals(request.getContractId(), response.getContractId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(expectedCashFlows.size(), response.getCashFlows().size());
        assertEquals(expectedSettlements.size(), response.getSettlementInstructions().size());

        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
        verify(settlementService).generateSettlementInstructions(anyList(), any(CashFlowRequestContent.class));
        verify(eventPublisher).publishCashFlowCalculatedEvent(anyString(), anyString(), anyInt(), any(Double.class), anyString());
        verify(cacheService).put(anyString(), any(CashFlowResponse.class));
    }

    @Test
    @DisplayName("Should return cached result when available")
    void shouldReturnCachedResultWhenAvailable() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        CashFlowResponse cachedResponse = createSampleResponse();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(cachedResponse);

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals(cachedResponse.getRequestId(), response.getRequestId());

        verify(validator).validateRequest(request);
        verify(engine, never()).calculateCashFlows(any());
        verify(settlementService, never()).generateSettlementInstructions(any(), any());
        verify(eventPublisher, never()).publishCashFlowCalculatedEvent(anyString(), anyString(), anyInt(), any(Double.class), anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when request is invalid")
    void shouldThrowValidationExceptionWhenRequestIsInvalid() {
        // Given
        CashFlowRequestContent request = createInvalidRequest();
        when(validator.validateRequest(any())).thenThrow(new RuntimeException("Invalid request"));

        // When & Then
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        
        assertThrows(java.util.concurrent.CompletionException.class, () -> future.join());
        
        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine, never()).calculateCashFlows(any());
        verify(settlementService, never()).generateSettlementInstructions(any(), any());
    }

    @Test
    @DisplayName("Should determine calculation type correctly")
    void shouldDetermineCalculationTypeCorrectly() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        request.setCalculationType(null); // Let service determine

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(createSampleCashFlows());
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(createSampleSettlements());

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertNotNull(response.getCalculationType());
        assertTrue(response.getCalculationType().equals("HISTORICAL_RECALCULATION") || 
                  response.getCalculationType().equals("INCREMENTAL_UPDATE") ||
                  response.getCalculationType().equals("REAL_TIME_PROCESSING"));
    }

    @Test
    @DisplayName("Should calculate cash flows in batch successfully")
    void shouldCalculateCashFlowsInBatchSuccessfully() {
        // Given
        List<CashFlowRequestContent> requests = List.of(createValidRequest(), createValidRequest());
        List<CashFlowResponse.CashFlow> expectedCashFlows = createSampleCashFlows();
        List<CashFlowResponse.SettlementInstruction> expectedSettlements = createSampleSettlements();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(expectedCashFlows);
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(expectedSettlements);

        // When
        List<CompletableFuture<CashFlowResponse>> futures = service.calculateCashFlowsBatch(requests);
        List<CashFlowResponse> responses = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        // Then
        assertEquals(2, responses.size());
        responses.forEach(response -> {
            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
        });

        verify(validator, times(2)).validateRequest(any(CashFlowRequestContent.class));
        verify(engine, times(2)).calculateCashFlows(any(CashFlowRequestContent.class));
    }

    @Test
    @DisplayName("Should recalculate cash flows for specific date range")
    void shouldRecalculateCashFlowsForSpecificDateRange() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        request.setCalculationType("HISTORICAL_RECALCULATION");
        List<CashFlowResponse.CashFlow> expectedCashFlows = createSampleCashFlows();
        List<CashFlowResponse.SettlementInstruction> expectedSettlements = createSampleSettlements();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(expectedCashFlows);
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(expectedSettlements);
        when(cacheService.evict(anyString())).thenReturn(true);

        // When
        CompletableFuture<CashFlowResponse> future = service.recalculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals("HISTORICAL_RECALCULATION", response.getCalculationType());
        assertEquals("SUCCESS", response.getStatus());

        verify(cacheService).evict(anyString());
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
    }

    @Test
    @DisplayName("Should get cached calculation by request ID")
    void shouldGetCachedCalculationByRequestId() {
        // Given
        String requestId = "TEST_REQUEST_123";
        CashFlowResponse cachedResponse = createSampleResponse();
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(cachedResponse);

        // When
        CashFlowResponse response = service.getCachedCalculation(requestId);

        // Then
        assertNotNull(response);
        assertEquals(cachedResponse.getRequestId(), response.getRequestId());

        verify(cacheService).get("cashflow:" + requestId, CashFlowResponse.class);
    }

    @Test
    @DisplayName("Should handle engine calculation errors gracefully")
    void shouldHandleEngineCalculationErrorsGracefully() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenThrow(new RuntimeException("Calculation failed"));

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Calculation failed"));

        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
        verify(settlementService, never()).generateSettlementInstructions(any(), any());
    }

    @Test
    @DisplayName("Should handle settlement service errors gracefully")
    void shouldHandleSettlementServiceErrorsGracefully() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        List<CashFlowResponse.CashFlow> expectedCashFlows = createSampleCashFlows();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(expectedCashFlows);
        when(settlementService.generateSettlementInstructions(any(), any())).thenThrow(new RuntimeException("Settlement failed"));

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Settlement failed"));

        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
        verify(settlementService).generateSettlementInstructions(anyList(), any(CashFlowRequestContent.class));
    }

    @Test
    @DisplayName("Should calculate summary correctly")
    void shouldCalculateSummaryCorrectly() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        List<CashFlowResponse.CashFlow> expectedCashFlows = createSampleCashFlows();
        List<CashFlowResponse.SettlementInstruction> expectedSettlements = createSampleSettlements();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(expectedCashFlows);
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(expectedSettlements);

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalAmount() > 0);
        assertNotNull(response.getCurrency());
        assertTrue(response.getCalculationDuration() > 0);
    }

    // Helper methods
    private CashFlowRequestContent createValidRequest() {
        return CashFlowRequestContent.builder()
            .requestId("TEST_REQUEST_" + UUID.randomUUID().toString().substring(0, 8))
            .contractId("SWAP_IBM_001")
            .dateRange(CashFlowRequestContent.DateRange.builder()
                .fromDate(LocalDate.of(2025, 8, 1))
                .toDate(LocalDate.of(2025, 8, 31))
                .calculationFrequency("DAILY")
                .build())
            .calculationType("HISTORICAL_RECALCULATION")
            .build();
    }

    private CashFlowRequestContent createInvalidRequest() {
        return CashFlowRequestContent.builder()
            .requestId("INVALID_REQUEST")
            .contractId(null) // Invalid - missing contract ID
            .build();
    }

    private List<CashFlowResponse.CashFlow> createSampleCashFlows() {
        return List.of(
            CashFlowResponse.CashFlow.builder()
                .cashFlowId("CF_001")
                .contractId("SWAP_IBM_001")
                .cashFlowType("INTEREST")
                .amount(5000.0)
                .currency("USD")
                .status("ACCRUAL")
                .build(),
            CashFlowResponse.CashFlow.builder()
                .cashFlowId("CF_002")
                .contractId("SWAP_IBM_001")
                .cashFlowType("DIVIDEND")
                .amount(3333.34)
                .currency("USD")
                .status("REALIZED_UNSETTLED")
                .build()
        );
    }

    private List<CashFlowResponse.SettlementInstruction> createSampleSettlements() {
        return List.of(
            CashFlowResponse.SettlementInstruction.builder()
                .settlementId("SETTLE_001")
                .contractId("SWAP_IBM_001")
                .cashFlowId("CF_002")
                .settlementDate(LocalDate.of(2025, 8, 31))
                .settlementType("DIVIDEND")
                .amount(3333.34)
                .currency("USD")
                .status("PENDING")
                .build()
        );
    }

    private CashFlowResponse createSampleResponse() {
        return CashFlowResponse.builder()
            .requestId("TEST_REQUEST_123")
            .contractId("SWAP_IBM_001")
            .calculationId(UUID.randomUUID().toString())
            .calculationType("HISTORICAL_RECALCULATION")
            .status("SUCCESS")
            .totalAmount(8333.34)
            .currency("USD")
            .calculationDuration(1500L)
            .cashFlows(createSampleCashFlows())
            .settlementInstructions(createSampleSettlements())
            .build();
    }
}
