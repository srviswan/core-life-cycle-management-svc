package com.financial.cashflow.fixtures;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test data fixtures for real-time cash flow calculations
 * Provides various scenarios for testing different contract types and market conditions
 */
@Component
public class TestDataFixtures {

    /**
     * Creates a standard IBM equity swap request
     */
    public CashFlowRequest createIbmEquitySwapRequest() {
        return CashFlowRequest.builder()
                .requestId("IBM_EQ_SWAP_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .dateRange(CashFlowRequest.DateRange.builder()
                        .fromDate(LocalDate.of(2024, 1, 15))
                        .toDate(LocalDate.of(2024, 1, 15))
                        .build())
                .calculationType(CashFlowRequest.CalculationType.REAL_TIME_PROCESSING)
                .contracts(new ArrayList<>(Arrays.asList(
                        CashFlowRequest.Contract.builder()
                                .contractId("IBM_EQ_SWAP_001")
                                .underlying("IBM")
                                .index("LIBOR_3M")
                                .notionalAmount(1000000.0)
                                .currency("USD")
                                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 1, 31))
                                .build()
                )))
                .marketDataStrategy(CashFlowRequest.MarketDataStrategy.builder()
                        .mode(CashFlowRequest.MarketDataStrategy.MarketDataMode.SELF_CONTAINED)
                        .build())
                .marketData(createIbmMarketData())
                .build();
    }

    /**
     * Creates a large notional equity swap request
     */
    public CashFlowRequest createLargeNotionalEquitySwapRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("LARGE_NOTIONAL_001");
        request.getContracts().get(0).setContractId("LARGE_NOTIONAL_001");
        request.getContracts().get(0).setNotionalAmount(10000000.0); // $10M
        return request;
    }

    /**
     * Creates a multi-contract request with different underlyings
     */
    public CashFlowRequest createMultiUnderlyingRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("MULTI_UNDERLYING_001");
        
        // Add AAPL contract
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("AAPL_EQ_SWAP_001")
                        .underlying("AAPL")
                        .index("LIBOR_3M")
                                .notionalAmount(500000.0)
                                .currency("USD")
                                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );

        // Add MSFT contract
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("MSFT_EQ_SWAP_001")
                        .underlying("MSFT")
                        .index("LIBOR_3M")
                                .notionalAmount(750000.0)
                                .currency("USD")
                                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );

        // Update market data to include all underlyings
        request.setMarketData(createMultiUnderlyingMarketData());
        return request;
    }

    /**
     * Creates a mixed contract types request
     */
    public CashFlowRequest createMixedContractTypesRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("MIXED_TYPES_001");
        
        // Add interest rate swap
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("IRS_001")
                        .underlying("USD")
                        .index("LIBOR_3M")
                                .notionalAmount(5000000.0)
                                .currency("USD")
                                .type(CashFlowRequest.Contract.ContractType.INTEREST_RATE_SWAP)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );

        // Add bond
        request.getContracts().add(
                CashFlowRequest.Contract.builder()
                        .contractId("BOND_001")
                        .underlying("GOVT_BOND")
                        .index("LIBOR_3M")
                                .notionalAmount(2000000.0)
                                .currency("USD")
                                .type(CashFlowRequest.Contract.ContractType.BOND)
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2024, 1, 31))
                        .build()
        );

        return request;
    }

    /**
     * Creates a EUR-based request
     */
    public CashFlowRequest createEurBasedRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("EUR_BASED_001");
        request.getContracts().get(0).setCurrency("EUR");
        request.getContracts().get(0).setIndex("LIBOR_3M"); // Use available rate data
        request.getMarketData().getData().getRates().get(0).setIndex("LIBOR_3M");
        request.getMarketData().getData().getRates().get(0).setBaseRate(0.0350); // 3.5%
        return request;
    }

    /**
     * Creates a request with different date ranges
     */
    public CashFlowRequest createDifferentDateRangeRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("DATE_RANGE_001");
        request.getDateRange().setFromDate(LocalDate.of(2024, 1, 1));
        request.getDateRange().setToDate(LocalDate.of(2024, 1, 31)); // Full month
        return request;
    }

    /**
     * Creates a request with endpoints market data strategy
     */
    public CashFlowRequest createEndpointsMarketDataRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("ENDPOINTS_001");
        request.getMarketDataStrategy().setMode(CashFlowRequest.MarketDataStrategy.MarketDataMode.ENDPOINTS);
        request.setMarketData(null); // No self-contained data
        return request;
    }

    /**
     * Creates a request with high volatility market data
     */
    public CashFlowRequest createHighVolatilityRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("HIGH_VOL_001");
        request.getMarketData().getData().getSecurities().get(0).setBasePrice(200.0); // High price
        request.getMarketData().getData().getRates().get(0).setBaseRate(0.0750); // High rate 7.5%
        return request;
    }

    /**
     * Creates a request with low volatility market data
     */
    public CashFlowRequest createLowVolatilityRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("LOW_VOL_001");
        request.getMarketData().getData().getSecurities().get(0).setBasePrice(50.0); // Low price
        request.getMarketData().getData().getRates().get(0).setBaseRate(0.0100); // Low rate 1%
        return request;
    }

    /**
     * Creates a request with dividend-paying stock
     */
    public CashFlowRequest createDividendPayingRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("DIVIDEND_001");
        request.getMarketData().getData().setDividends(Arrays.asList(
                CashFlowRequest.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                CashFlowRequest.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 3, 15))
                                        .amount(1.65)
                                        .currency("USD")
                                        .build()
                        ))
                        .build()
        ));
        return request;
    }

    /**
     * Creates a request with leap year dates
     */
    public CashFlowRequest createLeapYearRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("LEAP_YEAR_001");
        request.getContracts().get(0).setStartDate(LocalDate.of(2024, 1, 1));
        request.getContracts().get(0).setEndDate(LocalDate.of(2024, 2, 29)); // Leap year February
        return request;
    }

    /**
     * Creates a request with zero notional amount
     */
    public CashFlowRequest createZeroNotionalRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("ZERO_NOTIONAL_001");
        request.getContracts().get(0).setNotionalAmount(0.0);
        return request;
    }

    /**
     * Creates a request with null notional amount
     */
    public CashFlowRequest createNullNotionalRequest() {
        CashFlowRequest request = createIbmEquitySwapRequest();
        request.setRequestId("NULL_NOTIONAL_001");
        request.getContracts().get(0).setNotionalAmount(null);
        return request;
    }

    /**
     * Creates IBM market data
     */
    private CashFlowRequest.MarketDataContainer createIbmMarketData() {
        return CashFlowRequest.MarketDataContainer.builder()
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
                .build();
    }

    /**
     * Creates multi-underlying market data
     */
    private CashFlowRequest.MarketDataContainer createMultiUnderlyingMarketData() {
        return CashFlowRequest.MarketDataContainer.builder()
                .data(CashFlowRequest.MarketDataContent.builder()
                        .securities(Arrays.asList(
                                CashFlowRequest.SecurityData.builder()
                                        .symbol("IBM")
                                        .basePrice(125.50)
                                        .baseDate(LocalDate.of(2024, 1, 15))
                                        .changes(Arrays.asList())
                                        .build(),
                                CashFlowRequest.SecurityData.builder()
                                        .symbol("AAPL")
                                        .basePrice(180.25)
                                        .baseDate(LocalDate.of(2024, 1, 15))
                                        .changes(Arrays.asList())
                                        .build(),
                                CashFlowRequest.SecurityData.builder()
                                        .symbol("MSFT")
                                        .basePrice(350.75)
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
                                        .build(),
                                CashFlowRequest.DividendData.builder()
                                        .symbol("AAPL")
                                        .dividends(Arrays.asList())
                                        .build(),
                                CashFlowRequest.DividendData.builder()
                                        .symbol("MSFT")
                                        .dividends(Arrays.asList())
                                        .build()
                        ))
                        .build())
                .version("1.0")
                .asOfDate(LocalDate.of(2024, 1, 15))
                .build();
    }

    /**
     * Creates test market data for unit tests
     */
    public MarketData createTestMarketData() {
        return MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(125.50)
                        .baseDate(LocalDate.now())
                        .build())
                .rate(MarketData.RateData.builder()
                        .index("LIBOR_3M")
                        .baseRate(0.0525)
                        .baseDate(LocalDate.now())
                        .build())
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList())
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();
    }
}
