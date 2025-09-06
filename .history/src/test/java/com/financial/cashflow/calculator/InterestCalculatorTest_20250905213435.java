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
 * Unit tests for InterestCalculator
 * Tests real-time interest calculations for different contract types
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Interest Calculator Tests")
class InterestCalculatorTest {

    @InjectMocks
    private InterestCalculator interestCalculator;

    private MarketData marketData;
    private CashFlowRequest.Contract equitySwapContract;
    private CashFlowRequest.Contract interestRateSwapContract;
    private CashFlowRequest.Contract bondContract;

    @BeforeEach
    void setUp() {
        // Setup market data with LIBOR 3M rate
        marketData = MarketData.builder()
                .rate(MarketData.RateData.builder()
                        .index("LIBOR_3M")
                        .baseRate(0.0525) // 5.25%
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
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        // Setup interest rate swap contract
        interestRateSwapContract = CashFlowRequest.Contract.builder()
                .contractId("IRS_001")
                .underlying("USD")
                .index("LIBOR_3M")
                .notionalAmount(5000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.INTEREST_RATE_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        // Setup bond contract
        bondContract = CashFlowRequest.Contract.builder()
                .contractId("BOND_001")
                .underlying("GOVT_BOND")
                .index("LIBOR_3M")
                .notionalAmount(2000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.BOND)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();
    }

    @Test
    @DisplayName("Should calculate interest for equity swap correctly")
    void testCalculateEquitySwapInterest() {
        // Act
        double interest = interestCalculator.calculateInterest(equitySwapContract, marketData);

        // Assert
        // Expected: 1,000,000 * 0.0525 * 30 days / 365 = ~4,315.07
        double expectedInterest = 1000000.0 * 0.0525 * 30.0 / 365.0;
        assertEquals(expectedInterest, interest, 0.01);
    }

    @Test
    @DisplayName("Should calculate interest for interest rate swap correctly")
    void testCalculateInterestRateSwapInterest() {
        // Act
        double interest = interestCalculator.calculateInterest(interestRateSwapContract, marketData);

        // Assert
        // Expected: 5,000,000 * 0.0525 * 30 days / 365 = ~21,575.34
        double expectedInterest = 5000000.0 * 0.0525 * 30.0 / 365.0;
        assertEquals(expectedInterest, interest, 0.01);
    }

    @Test
    @DisplayName("Should calculate interest for bond correctly")
    void testCalculateBondInterest() {
        // Act
        double interest = interestCalculator.calculateInterest(bondContract, marketData);

        // Assert
        // Expected: 2,000,000 * 0.0525 * 30 days / 365 = ~8,630.14
        double expectedInterest = 2000000.0 * 0.0525 * 30.0 / 365.0;
        assertEquals(expectedInterest, interest, 0.01);
    }

    @Test
    @DisplayName("Should return zero interest for unsupported contract type")
    void testCalculateInterestForUnsupportedType() {
        // Arrange
        CashFlowRequest.Contract unsupportedContract = CashFlowRequest.Contract.builder()
                .contractId("UNSUPPORTED_001")
                .underlying("TEST")
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_FORWARD)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        // Act
        double interest = interestCalculator.calculateInterest(unsupportedContract, marketData);

        // Assert
        assertEquals(0.0, interest);
    }

    @Test
    @DisplayName("Should handle null notional amount with default value")
    void testCalculateInterestWithNullNotional() {
        // Arrange
        CashFlowRequest.Contract contractWithNullNotional = CashFlowRequest.Contract.builder()
                .contractId("NULL_NOTIONAL_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .notionalAmount(null)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        // Act
        double interest = interestCalculator.calculateInterest(contractWithNullNotional, marketData);

        // Assert
        // Expected: 1,000,000 (default) * 0.0525 * 30 days / 365 = ~4,315.07
        double expectedInterest = 1000000.0 * 0.0525 * 30.0 / 365.0;
        assertEquals(expectedInterest, interest, 0.01);
    }

    @Test
    @DisplayName("Should throw exception when rate data not found")
    void testCalculateInterestWithMissingRateData() {
        // Arrange
        MarketData marketDataWithoutRate = MarketData.builder()
                .rate(null)
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                interestCalculator.calculateInterest(equitySwapContract, marketDataWithoutRate));
        
        assertTrue(exception.getMessage().contains("Rate data not found for index: LIBOR_3M"));
    }

    @Test
    @DisplayName("Should throw exception when index mismatch")
    void testCalculateInterestWithIndexMismatch() {
        // Arrange
        MarketData marketDataWithDifferentIndex = MarketData.builder()
                .rate(MarketData.RateData.builder()
                        .index("SOFR")
                        .baseRate(0.0450)
                        .baseDate(LocalDate.now())
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("TEST")
                .isValid(true)
                .validUntil(java.time.LocalDateTime.now().plusHours(24))
                .build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                interestCalculator.calculateInterest(equitySwapContract, marketDataWithDifferentIndex));
        
        assertTrue(exception.getMessage().contains("Rate data not found for index: LIBOR_3M"));
    }

    @Test
    @DisplayName("Should calculate interest for different date ranges")
    void testCalculateInterestForDifferentDateRanges() {
        // Test 1-day period
        CashFlowRequest.Contract oneDayContract = CashFlowRequest.Contract.builder()
                .contractId("ONE_DAY_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 2))
                .build();

        double oneDayInterest = interestCalculator.calculateInterest(oneDayContract, marketData);
        double expectedOneDay = 1000000.0 * 0.0525 * 1.0 / 365.0;
        assertEquals(expectedOneDay, oneDayInterest, 0.01);

        // Test 365-day period (full year)
        CashFlowRequest.Contract oneYearContract = CashFlowRequest.Contract.builder()
                .contractId("ONE_YEAR_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        double oneYearInterest = interestCalculator.calculateInterest(oneYearContract, marketData);
        double expectedOneYear = 1000000.0 * 0.0525; // Full year
        assertEquals(expectedOneYear, oneYearInterest, 0.01);
    }

    @Test
    @DisplayName("Should handle leap year correctly")
    void testCalculateInterestForLeapYear() {
        // Arrange - 2024 is a leap year
        CashFlowRequest.Contract leapYearContract = CashFlowRequest.Contract.builder()
                .contractId("LEAP_YEAR_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 2, 29)) // 59 days in leap year
                .build();

        // Act
        double interest = interestCalculator.calculateInterest(leapYearContract, marketData);

        // Assert
        // Expected: 1,000,000 * 0.0525 * 59 days / 365 = ~8,486.30
        double expectedInterest = 1000000.0 * 0.0525 * 59.0 / 365.0;
        assertEquals(expectedInterest, interest, 0.01);
    }
}
