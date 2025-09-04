package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.exception.CashFlowCalculationException;
import com.financial.cashflow.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test cases for CashFlowCalculationService.
 */
@ExtendWith(MockitoExtension.class)
class CashFlowCalculationServiceTest {

    @Mock
    private CashFlowValidator validator;

    @Mock
    private CashFlowEngine engine;

    @Mock
    private SettlementService settlementService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private CacheService cacheService;

    private CashFlowCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CashFlowCalculationService(
            validator, engine, settlementService, eventPublisher, cacheService);
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
        assertFalse(response.getSummary().getCacheHit());

        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
        verify(settlementService).generateSettlementInstructions(anyList(), any(CashFlowRequestContent.class));
        verify(eventPublisher).publishCashFlowCalculated(any(CashFlowResponse.class));
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
        verify(eventPublisher, never()).publishCashFlowCalculated(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when request is invalid")
    void shouldThrowValidationExceptionWhenRequestIsInvalid() {
        // Given
        CashFlowRequestContent request = createInvalidRequest();
        when(validator.validateRequest(any())).thenThrow(new ValidationException("Invalid request"));

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
        assertNotNull(response.getCalculationType());
        assertNotNull(response.getCalculationType().getDeterminedType());
        assertNotNull(response.getCalculationType().getReason());
        assertEquals(1.0, response.getCalculationType().getConfidence());
    }

    @Test
    @DisplayName("Should calculate cash flows in batch successfully")
    void shouldCalculateCashFlowsInBatchSuccessfully() {
        // Given
        List<CashFlowRequestContent> requests = List.of(
            createValidRequest(),
            createValidRequest()
        );

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(createSampleCashFlows());
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(createSampleSettlements());

        // When
        CompletableFuture<List<CashFlowResponse>> future = service.calculateCashFlowsBatch(requests);
        List<CashFlowResponse> responses = future.join();

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        responses.forEach(response -> {
            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
        });
    }

    @Test
    @DisplayName("Should recalculate cash flows for specific date range")
    void shouldRecalculateCashFlowsForSpecificDateRange() {
        // Given
        String contractId = "SWAP_IBM_001";
        CashFlowRequestContent.DateRange dateRange = CashFlowRequestContent.DateRange.builder()
            .fromDate(LocalDate.now().minusDays(30))
            .toDate(LocalDate.now())
            .calculationFrequency("DAILY")
            .build();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(createSampleCashFlows());
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(createSampleSettlements());

        // When
        CompletableFuture<CashFlowResponse> future = service.recalculateCashFlows(contractId, dateRange);
        CashFlowResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals(contractId, response.getContractId());
        assertEquals("HISTORICAL_RECALCULATION", response.getCalculationType().getDeterminedType());
    }

    @Test
    @DisplayName("Should get cached calculation by request ID")
    void shouldGetCachedCalculationByRequestId() {
        // Given
        String requestId = "TEST_REQUEST_001";
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
        when(engine.calculateCashFlows(any())).thenThrow(new RuntimeException("Engine error"));

        // When & Then
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        
        assertThrows(java.util.concurrent.CompletionException.class, () -> future.join());
        
        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
        verify(settlementService, never()).generateSettlementInstructions(any(), any());
    }

    @Test
    @DisplayName("Should handle settlement service errors gracefully")
    void shouldHandleSettlementServiceErrorsGracefully() {
        // Given
        CashFlowRequestContent request = createValidRequest();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(createSampleCashFlows());
        when(settlementService.generateSettlementInstructions(any(), any()))
            .thenThrow(new RuntimeException("Settlement error"));

        // When & Then
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        
        assertThrows(java.util.concurrent.CompletionException.class, () -> future.join());
        
        verify(validator).validateRequest(any(CashFlowRequestContent.class));
        verify(engine).calculateCashFlows(any(CashFlowRequestContent.class));
        verify(settlementService).generateSettlementInstructions(any(), any());
    }

    @Test
    @DisplayName("Should calculate summary correctly")
    void shouldCalculateSummaryCorrectly() {
        // Given
        CashFlowRequestContent request = createValidRequest();
        List<CashFlowResponse.CashFlow> cashFlows = createSampleCashFlows();
        List<CashFlowResponse.SettlementInstruction> settlements = createSampleSettlements();

        when(validator.validateRequest(any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(CashFlowResponse.class))).thenReturn(null);
        when(engine.calculateCashFlows(any())).thenReturn(cashFlows);
        when(settlementService.generateSettlementInstructions(any(), any())).thenReturn(settlements);

        // When
        CompletableFuture<CashFlowResponse> future = service.calculateCashFlows(request);
        CashFlowResponse response = future.join();

        // Then
        CashFlowResponse.CalculationSummary summary = response.getSummary();
        assertNotNull(summary);
        assertEquals(cashFlows.size(), summary.getTotalCashFlows());
        assertEquals(settlements.size(), summary.getSettlementInstructions());
        assertTrue(summary.getCalculationDuration() > 0);
    }

    // Helper methods to create test data

    private CashFlowRequestContent createValidRequest() {
        return CashFlowRequestContent.builder()
            .requestId("TEST_REQUEST_001")
            .contractId("SWAP_IBM_001")
            .calculationType("REAL_TIME_PROCESSING")
            .dateRange(CashFlowRequestContent.DateRange.builder()
                .fromDate(LocalDate.now().minusDays(7))
                .toDate(LocalDate.now())
                .calculationFrequency("DAILY")
                .build())
            .contracts(List.of(createSampleContract()))
            .positions(List.of(createSamplePosition()))
            .lots(List.of(createSampleLot()))
            .build();
    }

    private CashFlowRequestContent createInvalidRequest() {
        return CashFlowRequestContent.builder()
            .requestId("") // Invalid - empty request ID
            .contractId("SWAP_IBM_001")
            .build();
    }

    private CashFlowRequestContent.Contract createSampleContract() {
        return CashFlowRequestContent.Contract.builder()
            .contractId("SWAP_IBM_001")
            .contractType("SWAP")
            .notionalAmount(1000000.0)
            .currency("USD")
            .startDate(LocalDate.now().minusDays(30))
            .endDate(LocalDate.now().plusDays(365))
            .interestRate(0.05)
            .interestRateIndex("LIBOR")
            .build();
    }

    private CashFlowRequestContent.Position createSamplePosition() {
        return CashFlowRequestContent.Position.builder()
            .positionId("POS_001")
            .contractId("SWAP_IBM_001")
            .quantity(1000.0)
            .averagePrice(150.0)
            .currency("USD")
            .positionDate(LocalDate.now())
            .book("TRADING")
            .account("CLIENT_001")
            .build();
    }

    private CashFlowRequestContent.Lot createSampleLot() {
        return CashFlowRequestContent.Lot.builder()
            .lotId("LOT_001")
            .positionId("POS_001")
            .quantity(500.0)
            .price(150.0)
            .currency("USD")
            .tradeDate(LocalDate.now().minusDays(1))
            .settlementDate(LocalDate.now().plusDays(2))
            .costBasis(150.0)
            .unrealizedPnL(0.0)
            .build();
    }

    private List<CashFlowResponse.CashFlow> createSampleCashFlows() {
        return List.of(
            CashFlowResponse.CashFlow.builder()
                .cashFlowId("CF_001")
                .contractId("SWAP_IBM_001")
                .lotId("LOT_001")
                .cashFlowType("INTEREST")
                .cashFlowDate(LocalDate.now())
                .amount(1000.0)
                .currency("USD")
                .status("REALIZED_UNSETTLED")
                .calculationBasis("DAILY_CLOSE")
                .build(),
            CashFlowResponse.CashFlow.builder()
                .cashFlowId("CF_002")
                .contractId("SWAP_IBM_001")
                .lotId("LOT_001")
                .cashFlowType("DIVIDEND")
                .cashFlowDate(LocalDate.now())
                .amount(500.0)
                .currency("USD")
                .status("REALIZED_UNSETTLED")
                .calculationBasis("SCHEDULED")
                .build()
        );
    }

    private List<CashFlowResponse.SettlementInstruction> createSampleSettlements() {
        return List.of(
            CashFlowResponse.SettlementInstruction.builder()
                .settlementId("SETTLE_001")
                .contractId("SWAP_IBM_001")
                .cashFlowId("CF_001")
                .settlementDate(LocalDate.now().plusDays(2))
                .settlementType("INTEREST")
                .amount(1000.0)
                .currency("USD")
                .status("PENDING")
                .build()
        );
    }

    private CashFlowResponse createSampleResponse() {
        return CashFlowResponse.builder()
            .requestId("TEST_REQUEST_001")
            .contractId("SWAP_IBM_001")
            .calculationId("CALC_001")
            .calculationDate(java.time.LocalDateTime.now())
            .cashFlows(createSampleCashFlows())
            .settlementInstructions(createSampleSettlements())
            .status("SUCCESS")
            .build();
    }
}
