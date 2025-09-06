package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DividendCalculator
 * Tests real-time dividend calculations for different contract types
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dividend Calculator Tests")
class DividendCalculatorTest {

    @InjectMocks
    private DividendCalculator dividendCalculator;

    private MarketData marketData;
    private CashFlowRequest.Contract equitySwapContract;
    private CashFlowRequest.Contract interestRateSwapContract;
    private CashFlowRequest.Contract bondContract;

    @BeforeEach
    void setUp() {
        // Setup market data with IBM dividend data
        marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                new MarketData.DividendData.Dividend(LocalDate.of(2024, 3, 15), 1.65, "USD"),
                                new MarketData.DividendData.Dividend(LocalDate.of(2024, 6, 14), 1.65, "USD")
                        ))
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Setup equity swap contract
        equitySwapContract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .build();

        // Setup interest rate swap contract
        interestRateSwapContract = CashFlowRequest.Contract.builder()
                .contractId("IRS_001")
                .underlying("USD")
                .notionalAmount(5000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.INTEREST_RATE_SWAP)
                .build();

        // Setup bond contract
        bondContract = CashFlowRequest.Contract.builder()
                .contractId("BOND_001")
                .underlying("GOVT_BOND")
                .notionalAmount(2000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.BOND)
                .build();
    }

    @Test
    @DisplayName("Should calculate dividends for equity swap correctly")
    void testCalculateEquitySwapDividends() {
        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);

        // Assert
        // For equity swap, dividends are based on notional amount and dividend yield
        // Simplified calculation: notional * dividend_yield_factor
        double expectedDividends = 1000000.0 * 0.001; // 0.1% factor
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should return zero dividends for interest rate swap")
    void testCalculateInterestRateSwapDividends() {
        // Act
        double dividends = dividendCalculator.calculateDividends(interestRateSwapContract, marketData);

        // Assert
        // Interest rate swaps don't have dividends
        assertEquals(0.0, dividends);
    }

    @Test
    @DisplayName("Should return zero dividends for bond")
    void testCalculateBondDividends() {
        // Act
        double dividends = dividendCalculator.calculateDividends(bondContract, marketData);

        // Assert
        // Bonds don't have dividends (they have coupons, which are handled separately)
        assertEquals(0.0, dividends);
    }

    @Test
    @DisplayName("Should return zero dividends for unsupported contract type")
    void testCalculateDividendsForUnsupportedType() {
        // Arrange
        CashFlowRequest.Contract unsupportedContract = CashFlowRequest.Contract.builder()
                .contractId("UNSUPPORTED_001")
                .underlying("TEST")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_FORWARD)
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(unsupportedContract, marketData);

        // Assert
        assertEquals(0.0, dividends);
    }

    @Test
    @DisplayName("Should handle null notional amount with default value")
    void testCalculateDividendsWithNullNotional() {
        // Arrange
        CashFlowRequest.Contract contractWithNullNotional = CashFlowRequest.Contract.builder()
                .contractId("NULL_NOTIONAL_001")
                .underlying("IBM")
                .notionalAmount(null)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(contractWithNullNotional, marketData);

        // Assert
        // Expected: 1,000,000 (default) * 0.001 = 1,000
        double expectedDividends = 1000000.0 * 0.001;
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should handle null dividend data gracefully")
    void testCalculateDividendsWithNullDividendData() {
        // Arrange
        MarketData marketDataWithoutDividends = MarketData.builder()
                .dividends(null)
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketDataWithoutDividends);

        // Assert
        // Should return zero when no dividend data
        assertEquals(0.0, dividends);
    }

    @Test
    @DisplayName("Should handle empty dividend list")
    void testCalculateDividendsWithEmptyDividendList() {
        // Arrange
        MarketData marketDataWithEmptyDividends = marketData.toBuilder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList())
                        .build())
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketDataWithEmptyDividends);

        // Assert
        // Should return zero when no dividends
        assertEquals(0.0, dividends);
    }

    @Test
    @DisplayName("Should calculate dividends for different notional amounts")
    void testCalculateDividendsForDifferentNotionalAmounts() {
        // Test with small notional
        CashFlowRequest.Contract smallNotionalContract = equitySwapContract.toBuilder()
                .notionalAmount(BigDecimal.valueOf(100000.0))
                .build();

        double smallDividends = dividendCalculator.calculateDividends(smallNotionalContract, marketData);
        double expectedSmallDividends = 100000.0 * 0.001;
        assertEquals(expectedSmallDividends, smallDividends, 0.01);

        // Test with large notional
        CashFlowRequest.Contract largeNotionalContract = equitySwapContract.toBuilder()
                .notionalAmount(BigDecimal.valueOf(10000000.0))
                .build();

        double largeDividends = dividendCalculator.calculateDividends(largeNotionalContract, marketData);
        double expectedLargeDividends = 10000000.0 * 0.001;
        assertEquals(expectedLargeDividends, largeDividends, 0.01);
    }

    @Test
    @DisplayName("Should calculate dividends for different currencies")
    void testCalculateDividendsForDifferentCurrencies() {
        // Test USD
        double usdDividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);
        assertTrue(usdDividends > 0);

        // Test EUR contract
        CashFlowRequest.Contract eurContract = equitySwapContract.toBuilder()
                .currency("EUR")
                .build();

        double eurDividends = dividendCalculator.calculateDividends(eurContract, marketData);
        // EUR should have different calculation (simplified)
        assertTrue(eurDividends > 0);
        assertNotEquals(usdDividends, eurDividends);
    }

    @Test
    @DisplayName("Should handle dividend data with different symbols")
    void testCalculateDividendsForDifferentSymbols() {
        // Test with IBM dividends
        double ibmDividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);
        assertTrue(ibmDividends > 0);

        // Test with different underlying but same dividend data
        CashFlowRequest.Contract aaplContract = equitySwapContract.toBuilder()
                .underlying("AAPL")
                .build();

        double aaplDividends = dividendCalculator.calculateDividends(aaplContract, marketData);
        // Should return zero since dividend data is for IBM, not AAPL
        assertEquals(0.0, aaplDividends);
    }

    @Test
    @DisplayName("Should handle multiple dividend payments")
    void testCalculateDividendsWithMultiplePayments() {
        // The market data already has 2 dividend payments for IBM
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);
        
        // Should calculate based on total dividend yield, not individual payments
        double expectedDividends = 1000000.0 * 0.001; // 0.1% factor
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should handle dividend data with different amounts")
    void testCalculateDividendsWithDifferentAmounts() {
        // Setup market data with different dividend amounts
        MarketData variedDividendMarketData = marketData.toBuilder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 3, 15))
                                        .paymentDate(LocalDate.of(2024, 3, 30))
                                        .amount(2.50) // Higher dividend
                                        .currency("USD")
                                        .build(),
                                MarketData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 6, 14))
                                        .paymentDate(LocalDate.of(2024, 6, 29))
                                        .amount(0.80) // Lower dividend
                                        .currency("USD")
                                        .build()
                        ))
                        .build())
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, variedDividendMarketData);

        // Assert
        // Should still use the same calculation factor
        double expectedDividends = 1000000.0 * 0.001;
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should handle future dividend dates")
    void testCalculateDividendsWithFutureDates() {
        // Setup market data with future dividend dates
        MarketData futureDividendMarketData = marketData.toBuilder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.Dividend.builder()
                                        .exDate(LocalDate.now().plusDays(30))
                                        .paymentDate(LocalDate.now().plusDays(45))
                                        .amount(1.65)
                                        .currency("USD")
                                        .build()
                        ))
                        .build())
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, futureDividendMarketData);

        // Assert
        // Should still calculate dividends regardless of future dates
        double expectedDividends = 1000000.0 * 0.001;
        assertEquals(expectedDividends, dividends, 0.01);
    }
}
