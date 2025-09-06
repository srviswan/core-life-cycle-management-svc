package com.financial.cashflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CashFlowController
 * Tests real-time cash flow calculation endpoints
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Cash Flow Controller Integration Tests")
class CashFlowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should calculate real-time cash flows successfully")
    void testCalculateRealTimeCashFlows() throws Exception {
        // Arrange
        CashFlowRequest request = createValidCashFlowRequest();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").value("CF_REQ_RT_001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.contractResults").isArray())
                .andExpect(jsonPath("$.contractResults[0].contractId").value("SWAP_IBM_001"))
                .andExpect(jsonPath("$.contractResults[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.summary.totalContracts").value(1))
                .andExpect(jsonPath("$.summary.totalCashFlows").exists())
                .andReturn();

        // Verify response structure
        String responseContent = result.getResponse().getContentAsString();
        CashFlowResponse response = objectMapper.readValue(responseContent, CashFlowResponse.class);
        
        assertNotNull(response.getRequestId());
        assertNotNull(response.getCalculationDate());
        assertNotNull(response.getSummary());
        assertNotNull(response.getContractResults());
        assertFalse(response.getContractResults().isEmpty());
        
        CashFlowResponse.ContractResult contractResult = response.getContractResults().get(0);
        assertEquals("SWAP_IBM_001", contractResult.getContractId());
        assertEquals("SUCCESS", contractResult.getStatus());
        assertNotNull(contractResult.getTotalPnl());
        assertNotNull(contractResult.getTotalInterest());
        assertNotNull(contractResult.getTotalDividends());
        assertNotNull(contractResult.getTotalCashFlows());
    }

    @Test
    @DisplayName("Should handle multiple contracts in real-time calculation")
    void testCalculateRealTimeCashFlowsMultipleContracts() throws Exception {
        // Arrange
        CashFlowRequest request = createMultiContractCashFlowRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.contractResults").isArray())
                .andExpect(jsonPath("$.contractResults.length()").value(2))
                .andExpect(jsonPath("$.summary.totalContracts").value(2))
                .andExpect(jsonPath("$.contractResults[0].contractId").value("SWAP_IBM_001"))
                .andExpect(jsonPath("$.contractResults[1].contractId").value("SWAP_AAPL_001"));
    }

    @Test
    @DisplayName("Should handle real-time calculation with different contract types")
    void testCalculateRealTimeCashFlowsDifferentTypes() throws Exception {
        // Arrange
        CashFlowRequest request = createMixedContractTypesRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.contractResults").isArray())
                .andExpect(jsonPath("$.contractResults.length()").value(3))
                .andExpect(jsonPath("$.summary.totalContracts").value(3));
    }

    @Test
    @DisplayName("Should handle real-time calculation with different market data strategies")
    void testCalculateRealTimeCashFlowsDifferentMarketDataStrategies() throws Exception {
        // Test SELF_CONTAINED strategy
        CashFlowRequest selfContainedRequest = createValidCashFlowRequest();
        selfContainedRequest.getMarketDataStrategy().setMode(CashFlowRequest.MarketDataStrategy.MarketDataMode.SELF_CONTAINED);

        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfContainedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Test ENDPOINTS strategy
        CashFlowRequest endpointsRequest = createValidCashFlowRequest();
        endpointsRequest.getMarketDataStrategy().setMode(CashFlowRequest.MarketDataStrategy.MarketDataMode.ENDPOINTS);
        endpointsRequest.setMarketData(null); // No self-contained data for endpoints mode

        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Should handle real-time calculation with different date ranges")
    void testCalculateRealTimeCashFlowsDifferentDateRanges() throws Exception {
        // Test single day
        CashFlowRequest singleDayRequest = createValidCashFlowRequest();
        singleDayRequest.getDateRange().setFromDate(LocalDate.of(2024, 1, 15));
        singleDayRequest.getDateRange().setToDate(LocalDate.of(2024, 1, 15));

        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleDayRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Test multiple days
        CashFlowRequest multiDayRequest = createValidCashFlowRequest();
        multiDayRequest.getDateRange().setFromDate(LocalDate.of(2024, 1, 15));
        multiDayRequest.getDateRange().setToDate(LocalDate.of(2024, 1, 20));

        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multiDayRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Should handle real-time calculation with different currencies")
    void testCalculateRealTimeCashFlowsDifferentCurrencies() throws Exception {
        // Test USD
        CashFlowRequest usdRequest = createValidCashFlowRequest();
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usdRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Test EUR
        CashFlowRequest eurRequest = createValidCashFlowRequest();
        eurRequest.getContracts().get(0).setCurrency("EUR");
        eurRequest.getMarketData().getData().getRates().get(0).setIndex("EURIBOR_3M");

        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eurRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Should return error for invalid request")
    void testCalculateRealTimeCashFlowsInvalidRequest() throws Exception {
        // Arrange - create request with missing required fields
        CashFlowRequest invalidRequest = new CashFlowRequest();
        invalidRequest.setRequestId("INVALID_REQUEST");

        // Act & Assert
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("Should return error for malformed JSON")
    void testCalculateRealTimeCashFlowsMalformedJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle real-time calculation performance")
    void testCalculateRealTimeCashFlowsPerformance() throws Exception {
        // Arrange
        CashFlowRequest request = createValidCashFlowRequest();
        long startTime = System.currentTimeMillis();

        // Act & Assert
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.metadata.processingTimeMs").exists());

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Real-time calculations should complete quickly (within 5 seconds)
        assertTrue(totalTime < 5000, "Real-time calculation should complete within 5 seconds, took: " + totalTime + "ms");
    }

    @Test
    @DisplayName("Should handle concurrent real-time calculations")
    void testCalculateRealTimeCashFlowsConcurrent() throws Exception {
        // Arrange
        CashFlowRequest request1 = createValidCashFlowRequest();
        request1.setRequestId("CONCURRENT_001");
        request1.getContracts().get(0).setContractId("CONCURRENT_CONTRACT_001");

        CashFlowRequest request2 = createValidCashFlowRequest();
        request2.setRequestId("CONCURRENT_002");
        request2.getContracts().get(0).setContractId("CONCURRENT_CONTRACT_002");

        // Act & Assert - both should succeed
        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/cashflows/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // Helper methods to create test data

    private CashFlowRequest createValidCashFlowRequest() {
        return CashFlowRequest.builder()
                .requestId("CF_REQ_RT_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .dateRange(CashFlowRequest.DateRange.builder()
                        .fromDate(LocalDate.of(2024, 1, 15))
                        .toDate(LocalDate.of(2024, 1, 15))
                        .build())
                .calculationType(CashFlowRequest.CalculationType.REAL_TIME_PROCESSING)
                .contracts(Arrays.asList(
                        CashFlowRequest.Contract.builder()
                                .contractId("SWAP_IBM_001")
                                .underlying("IBM")
                                .index("LIBOR_3M")
                                .notionalAmount(BigDecimal.valueOf(1000000.0))
                                .currency("USD")
                                .type(CashFlowRequest.ContractType.EQUITY_SWAP)
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 1, 31))
                                .build()
                ))
                .marketDataStrategy(CashFlowRequest.MarketDataStrategy.builder()
                        .mode(CashFlowRequest.MarketDataStrategy.Mode.SELF_CONTAINED)
                        .build())
                .marketData(CashFlowRequest.MarketDataContainer.builder()
                        .data(CashFlowRequest.MarketDataContent.builder()
                                .securities(Arrays.asList(
                                        CashFlowRequest.SecurityData.builder()
                                                .symbol("IBM")
                                                .basePrice(125.50)
                                                .baseDate(LocalDate.of(2024, 1, 15))
                                                .changes(Arrays.asList())
                                                .build()
                                ))
                                .rates(Arrays.asList(
                                        CashFlowRequest.RateData.builder()
                                                .index("LIBOR_3M")
                                                .baseRate(0.0525)
                                                .baseDate(LocalDate.of(2024, 1, 15))
                                                .changes(Arrays.asList())
                                                .build()
                                ))
                                .dividends(Arrays.asList(
                                        CashFlowRequest.DividendData.builder()
                                                .symbol("IBM")
                                                .dividends(Arrays.asList())
                                                .build()
                                ))
                                .build())
                        .version("1.0")
                        .asOfDate(LocalDate.of(2024, 1, 15))
                        .build())
                .build();
    }

    private CashFlowRequest createMultiContractCashFlowRequest() {
        CashFlowRequest request = createValidCashFlowRequest();
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("SWAP_AAPL_001")
                        .underlying("AAPL")
                        .index("LIBOR_3M")
                        .notionalAmount(BigDecimal.valueOf(500000.0))
                        .currency("USD")
                        .type(CashFlowRequest.ContractType.EQUITY_SWAP)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );
        return request;
    }

    private CashFlowRequest createMixedContractTypesRequest() {
        CashFlowRequest request = createValidCashFlowRequest();
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("IRS_001")
                        .underlying("USD")
                        .index("LIBOR_3M")
                        .notionalAmount(BigDecimal.valueOf(2000000.0))
                        .currency("USD")
                        .type(CashFlowRequest.ContractType.INTEREST_RATE_SWAP)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("BOND_001")
                        .underlying("GOVT_BOND")
                        .index("LIBOR_3M")
                        .notionalAmount(BigDecimal.valueOf(1000000.0))
                        .currency("USD")
                        .type(CashFlowRequest.ContractType.BOND)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );
        return request;
    }
}
