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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PnLCalculator
 * Tests real-time P&L calculations for different contract types
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PnL Calculator Tests")
class PnLCalculatorTest {

    @InjectMocks
    private PnLCalculator pnLCalculator;

    private MarketData marketData;
    private CashFlowRequest.Contract equitySwapContract;
    private CashFlowRequest.Contract interestRateSwapContract;
    private CashFlowRequest.Contract bondContract;

    @BeforeEach
    void setUp() {
        // Setup market data with IBM stock price
        marketData = MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(125.50)
                        .baseDate(LocalDate.now())
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
    @DisplayName("Should calculate P&L for equity swap correctly")
    void testCalculateEquitySwapPnL() {
        // Act
        double pnl = pnLCalculator.calculatePnL(equitySwapContract, marketData);

        // Assert
        // For equity swap, P&L is based on notional amount and current price
        // Simplified calculation: notional * (current_price / 100) * price_change_factor
        double expectedPnL = 1000000.0 * (125.50 / 100.0) * 0.02; // 2% price change factor
        assertEquals(expectedPnL, pnl, 0.01);
    }

    @Test
    @DisplayName("Should calculate P&L for interest rate swap correctly")
    void testCalculateInterestRateSwapPnL() {
        // Act
        double pnl = pnLCalculator.calculatePnL(interestRateSwapContract, marketData);

        // Assert
        // For interest rate swap, P&L is based on notional amount
        // Simplified calculation: notional * interest_rate_factor
        double expectedPnL = 5000000.0 * 0.001; // 0.1% factor
        assertEquals(expectedPnL, pnl, 0.01);
    }

    @Test
    @DisplayName("Should calculate P&L for bond correctly")
    void testCalculateBondPnL() {
        // Act
        double pnl = pnLCalculator.calculatePnL(bondContract, marketData);

        // Assert
        // For bond, P&L is based on notional amount and bond price
        // Simplified calculation: notional * bond_price_factor
        double expectedPnL = 2000000.0 * 0.005; // 0.5% factor
        assertEquals(expectedPnL, pnl, 0.01);
    }

    @Test
    @DisplayName("Should return zero P&L for unsupported contract type")
    void testCalculatePnLForUnsupportedType() {
        // Arrange
        CashFlowRequest.Contract unsupportedContract = CashFlowRequest.Contract.builder()
                .contractId("UNSUPPORTED_001")
                .underlying("TEST")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_FORWARD)
                .build();

        // Act
        double pnl = pnLCalculator.calculatePnL(unsupportedContract, marketData);

        // Assert
        assertEquals(0.0, pnl);
    }

    @Test
    @DisplayName("Should handle null notional amount with default value")
    void testCalculatePnLWithNullNotional() {
        // Arrange
        CashFlowRequest.Contract contractWithNullNotional = CashFlowRequest.Contract.builder()
                .contractId("NULL_NOTIONAL_001")
                .underlying("IBM")
                .notionalAmount(null)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .build();

        // Act
        double pnl = pnLCalculator.calculatePnL(contractWithNullNotional, marketData);

        // Assert
        // Expected: 1,000,000 (default) * (125.50 / 100.0) * 0.02 = ~25,100
        double expectedPnL = 1000000.0 * (125.50 / 100.0) * 0.02;
        assertEquals(expectedPnL, pnl, 0.01);
    }

    @Test
    @DisplayName("Should handle null price data gracefully")
    void testCalculatePnLWithNullPriceData() {
        // Arrange
        MarketData marketDataWithoutPrice = MarketData.builder()
                .price(null)
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act
        double pnl = pnLCalculator.calculatePnL(equitySwapContract, marketDataWithoutPrice);

        // Assert
        // Should return a default P&L value or zero
        assertEquals(0.0, pnl);
    }

    @Test
    @DisplayName("Should calculate P&L for different notional amounts")
    void testCalculatePnLForDifferentNotionalAmounts() {
        // Test with small notional
        CashFlowRequest.Contract smallNotionalContract = equitySwapContract.toBuilder()
                .notionalAmount(BigDecimal.valueOf(100000.0))
                .build();

        double smallPnL = pnLCalculator.calculatePnL(smallNotionalContract, marketData);
        double expectedSmallPnL = 100000.0 * (125.50 / 100.0) * 0.02;
        assertEquals(expectedSmallPnL, smallPnL, 0.01);

        // Test with large notional
        CashFlowRequest.Contract largeNotionalContract = equitySwapContract.toBuilder()
                .notionalAmount(BigDecimal.valueOf(10000000.0))
                .build();

        double largePnL = pnLCalculator.calculatePnL(largeNotionalContract, marketData);
        double expectedLargePnL = 10000000.0 * (125.50 / 100.0) * 0.02;
        assertEquals(expectedLargePnL, largePnL, 0.01);
    }

    @Test
    @DisplayName("Should calculate P&L for different currencies")
    void testCalculatePnLForDifferentCurrencies() {
        // Test USD
        double usdPnL = pnLCalculator.calculatePnL(equitySwapContract, marketData);
        assertTrue(usdPnL > 0);

        // Test EUR contract
        CashFlowRequest.Contract eurContract = equitySwapContract.toBuilder()
                .currency("EUR")
                .build();

        double eurPnL = pnLCalculator.calculatePnL(eurContract, marketData);
        // EUR should have different calculation (simplified)
        assertTrue(eurPnL > 0);
        assertNotEquals(usdPnL, eurPnL);
    }

    @Test
    @DisplayName("Should handle market data with different price levels")
    void testCalculatePnLForDifferentPriceLevels() {
        // Test with low price
        MarketData lowPriceMarketData = marketData.toBuilder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(50.0)
                        .baseDate(LocalDate.now())
                        .build())
                .build();

        double lowPricePnL = pnLCalculator.calculatePnL(equitySwapContract, lowPriceMarketData);
        assertTrue(lowPricePnL > 0);

        // Test with high price
        MarketData highPriceMarketData = marketData.toBuilder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(200.0)
                        .baseDate(LocalDate.now())
                        .build())
                .build();

        double highPricePnL = pnLCalculator.calculatePnL(equitySwapContract, highPriceMarketData);
        assertTrue(highPricePnL > 0);
        assertNotEquals(lowPricePnL, highPricePnL);
    }

    @Test
    @DisplayName("Should handle multiple contract calculations consistently")
    void testMultipleContractPnLCalculations() {
        // Calculate P&L for multiple contracts
        double equityPnL = pnLCalculator.calculatePnL(equitySwapContract, marketData);
        double interestPnL = pnLCalculator.calculatePnL(interestRateSwapContract, marketData);
        double bondPnL = pnLCalculator.calculatePnL(bondContract, marketData);

        // All should be positive and different
        assertTrue(equityPnL > 0);
        assertTrue(interestPnL > 0);
        assertTrue(bondPnL > 0);
        
        assertNotEquals(equityPnL, interestPnL);
        assertNotEquals(interestPnL, bondPnL);
        assertNotEquals(equityPnL, bondPnL);
    }
}
