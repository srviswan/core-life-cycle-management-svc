package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

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
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 3, 15))
                                        .paymentDate(LocalDate.of(2024, 3, 30))
                                        .amount(1.65)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build(),
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 6, 14))
                                        .paymentDate(LocalDate.of(2024, 6, 30))
                                        .amount(1.65)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
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
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Setup interest rate swap contract
        interestRateSwapContract = CashFlowRequest.Contract.builder()
                .contractId("IRS_001")
                .underlying("USD")
                .notionalAmount(5000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.INTEREST_RATE_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Setup bond contract
        bondContract = CashFlowRequest.Contract.builder()
                .contractId("BOND_001")
                .underlying("GOVT_BOND")
                .notionalAmount(2000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.BOND)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();
    }

    @Test
    @DisplayName("Should calculate dividends for equity swap correctly")
    void testCalculateEquitySwapDividends() {
        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);

        // Assert
        // Actual implementation: notional * (dividend1_amount + dividend2_amount) / 1000000.0
        // Expected: 1000000.0 * (1.65 + 1.65) / 1000000.0 = 3.30
        double expectedDividends = 1000000.0 * (1.65 + 1.65) / 1000000.0;
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should throw exception for interest rate swap (no dividend data)")
    void testCalculateInterestRateSwapDividends() {
        // Act & Assert - Interest rate swaps don't have dividend data for underlying "USD"
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dividendCalculator.calculateDividends(interestRateSwapContract, marketData));

        assertTrue(exception.getMessage().contains("Dividend calculation failed"),
                "Expected exception message to contain 'Dividend calculation failed', but was: " + exception.getMessage());

        // Check that the cause contains the original error
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Dividend data not found for underlying: USD"),
                "Expected cause message to contain 'Dividend data not found for underlying: USD', but was: " + exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should throw exception for bond (no dividend data)")
    void testCalculateBondDividends() {
        // Act & Assert - Bonds don't have dividend data for underlying "GOVT_BOND"
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dividendCalculator.calculateDividends(bondContract, marketData));

        assertTrue(exception.getMessage().contains("Dividend calculation failed"),
                "Expected exception message to contain 'Dividend calculation failed', but was: " + exception.getMessage());

        // Check that the cause contains the original error
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Dividend data not found for underlying: GOVT_BOND"),
                "Expected cause message to contain 'Dividend data not found for underlying: GOVT_BOND', but was: " + exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should throw exception for unsupported contract type (no dividend data)")
    void testCalculateDividendsForUnsupportedType() {
        // Arrange
        CashFlowRequest.Contract unsupportedContract = CashFlowRequest.Contract.builder()
                .contractId("UNSUPPORTED_001")
                .underlying("TEST")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_FORWARD)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Act & Assert - No dividend data for underlying "TEST"
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dividendCalculator.calculateDividends(unsupportedContract, marketData));

        assertTrue(exception.getMessage().contains("Dividend calculation failed"),
                "Expected exception message to contain 'Dividend calculation failed', but was: " + exception.getMessage());

        // Check that the cause contains the original error
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Dividend data not found for underlying: TEST"),
                "Expected cause message to contain 'Dividend data not found for underlying: TEST', but was: " + exception.getCause().getMessage());
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
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(contractWithNullNotional, marketData);

        // Assert
        // Expected: 1,000,000 (default) * (1.65 + 1.65) / 1000000.0 = 3.30
        double expectedDividends = 1000000.0 * (1.65 + 1.65) / 1000000.0;
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should throw exception for null dividend data")
    void testCalculateDividendsWithNullDividendData() {
        // Arrange
        MarketData marketDataWithoutDividends = MarketData.builder()
                .dividends(null)
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act & Assert - Should throw exception when no dividend data
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dividendCalculator.calculateDividends(equitySwapContract, marketDataWithoutDividends));

        assertTrue(exception.getMessage().contains("Dividend calculation failed"),
                "Expected exception message to contain 'Dividend calculation failed', but was: " + exception.getMessage());

        // Check that the cause contains the original error
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Dividend data not found for underlying: IBM"),
                "Expected cause message to contain 'Dividend data not found for underlying: IBM', but was: " + exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle empty dividend list")
    void testCalculateDividendsWithEmptyDividendList() {
        // Arrange
        MarketData marketDataWithEmptyDividends = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList())
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketDataWithEmptyDividends);

        // Assert
        // Should return zero when no dividends in the list
        assertEquals(0.0, dividends);
    }

    @Test
    @DisplayName("Should calculate dividends for different notional amounts")
    void testCalculateDividendsForDifferentNotionalAmounts() {
        // Test with small notional
        CashFlowRequest.Contract smallNotionalContract = CashFlowRequest.Contract.builder()
                .contractId("SMALL_NOTIONAL_001")
                .underlying("IBM")
                .notionalAmount(100000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        double smallDividends = dividendCalculator.calculateDividends(smallNotionalContract, marketData);
        double expectedSmallDividends = 100000.0 * (1.65 + 1.65) / 1000000.0;
        assertEquals(expectedSmallDividends, smallDividends, 0.01);

        // Test with large notional
        CashFlowRequest.Contract largeNotionalContract = CashFlowRequest.Contract.builder()
                .contractId("LARGE_NOTIONAL_001")
                .underlying("IBM")
                .notionalAmount(10000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        double largeDividends = dividendCalculator.calculateDividends(largeNotionalContract, marketData);
        double expectedLargeDividends = 10000000.0 * (1.65 + 1.65) / 1000000.0;
        assertEquals(expectedLargeDividends, largeDividends, 0.01);
    }

    @Test
    @DisplayName("Should calculate dividends for different currencies")
    void testCalculateDividendsForDifferentCurrencies() {
        // Test USD
        double usdDividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);
        assertTrue(usdDividends > 0);

        // Test EUR contract
        CashFlowRequest.Contract eurContract = CashFlowRequest.Contract.builder()
                .contractId("EUR_CONTRACT_001")
                .underlying("IBM")
                .notionalAmount(1000000.0)
                .currency("EUR")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        double eurDividends = dividendCalculator.calculateDividends(eurContract, marketData);
        // EUR should have same calculation (currency doesn't affect dividend calculation in implementation)
        assertTrue(eurDividends > 0);
        assertEquals(usdDividends, eurDividends, 0.01);
    }

    @Test
    @DisplayName("Should handle dividend data with different symbols")
    void testCalculateDividendsForDifferentSymbols() {
        // Test with IBM dividends
        double ibmDividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);
        assertTrue(ibmDividends > 0);

        // Test with different underlying but same dividend data
        CashFlowRequest.Contract aaplContract = CashFlowRequest.Contract.builder()
                .contractId("AAPL_CONTRACT_001")
                .underlying("AAPL")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Act & Assert - Should throw exception since dividend data is for IBM, not AAPL
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dividendCalculator.calculateDividends(aaplContract, marketData));

        assertTrue(exception.getMessage().contains("Dividend calculation failed"),
                "Expected exception message to contain 'Dividend calculation failed', but was: " + exception.getMessage());

        // Check that the cause contains the original error
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Dividend data not found for underlying: AAPL"),
                "Expected cause message to contain 'Dividend data not found for underlying: AAPL', but was: " + exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle multiple dividend payments")
    void testCalculateDividendsWithMultiplePayments() {
        // The market data already has 2 dividend payments for IBM
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, marketData);
        
        // Should calculate based on actual dividend amounts: notional * (dividend1 + dividend2) / 1000000.0
        double expectedDividends = 1000000.0 * (1.65 + 1.65) / 1000000.0;
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should handle dividend data with different amounts")
    void testCalculateDividendsWithDifferentAmounts() {
        // Setup market data with different dividend amounts
        MarketData variedDividendMarketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 3, 15))
                                        .paymentDate(LocalDate.of(2024, 3, 30))
                                        .amount(2.50)
                                        .currency("USD")
                                        .build(),
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 6, 14))
                                        .paymentDate(LocalDate.of(2024, 6, 30))
                                        .amount(0.80)
                                        .currency("USD")
                                        .build()
                        ))
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, variedDividendMarketData);

        // Assert
        // Should calculate based on actual dividend amounts: notional * (2.50 + 0.80) / 1000000.0
        double expectedDividends = 1000000.0 * (2.50 + 0.80) / 1000000.0;
        assertEquals(expectedDividends, dividends, 0.01);
    }

    @Test
    @DisplayName("Should handle future dividend dates")
    void testCalculateDividendsWithFutureDates() {
        // Setup market data with future dividend dates within contract period
        MarketData futureDividendMarketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 6, 15))
                                        .paymentDate(LocalDate.of(2024, 6, 30))
                                        .amount(1.65)
                                        .currency("USD")
                                        .build()
                        ))
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act
        double dividends = dividendCalculator.calculateDividends(equitySwapContract, futureDividendMarketData);

        // Assert
        // Should still calculate dividends based on actual dividend amounts: notional * 1.65 / 1000000.0
        double expectedDividends = 1000000.0 * 1.65 / 1000000.0;
        assertEquals(expectedDividends, dividends, 0.01);
    }
}
