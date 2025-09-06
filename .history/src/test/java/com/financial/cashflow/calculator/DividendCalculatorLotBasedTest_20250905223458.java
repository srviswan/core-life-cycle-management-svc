package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for lot-based dividend calculation logic
 */
@ExtendWith(MockitoExtension.class)
class DividendCalculatorLotBasedTest {

    @InjectMocks
    private DividendCalculator dividendCalculator;

    private CashFlowRequest.Contract testContract;
    private MarketData testMarketData;
    private List<MarketData.DividendData.Dividend> testDividends;

    @BeforeEach
    void setUp() {
        // Create test contract
        testContract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Create test dividends
        testDividends = Arrays.asList(
                MarketData.DividendData.Dividend.builder()
                        .exDate(LocalDate.of(2024, 1, 10))
                        .paymentDate(LocalDate.of(2024, 1, 15))
                        .amount(1.65)
                        .currency("USD")
                        .build(),
                MarketData.DividendData.Dividend.builder()
                        .exDate(LocalDate.of(2024, 1, 20))
                        .paymentDate(LocalDate.of(2024, 1, 25))
                        .amount(1.50)
                        .currency("USD")
                        .build(),
                MarketData.DividendData.Dividend.builder()
                        .exDate(LocalDate.of(2024, 2, 10))
                        .paymentDate(LocalDate.of(2024, 2, 15))
                        .amount(1.75)
                        .currency("USD")
                        .build()
        );

        // Create test market data
        testMarketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(testDividends)
                        .build())
                .build();
    }

    @Test
    void testLotBasedDividendCalculation_ValidLotsAndDividends() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build(),
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_002")
                        .contractId("EQ_SWAP_001")
                        .quantity(500.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Only dividend with exDate 2024-01-10 should be considered (exDate <= 2024-01-15)
        // Total quantity = 1000 + 500 = 1500
        // Expected dividend = 1500 * 1.65 = 2475.0
        assertEquals(2475.0, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_NoValidLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 20)) // Cost date after calculation date
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // No lots have valid cost dates, so should fall back to contract-based calculation
        // Contract-based: notional * dividend_amount / 1000000.0
        // All dividends are in contract period: 1.65 + 1.50 + 1.75 = 4.9
        assertEquals(4.9, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_NoLotsProvided() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Collections.emptyList();

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Should fall back to contract-based calculation
        // All dividends are in contract period: 1.65 + 1.50 + 1.75 = 4.9
        assertEquals(4.9, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_NullLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = null;

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Should fall back to contract-based calculation
        // All dividends are in contract period: 1.65 + 1.50 + 1.75 = 4.9
        assertEquals(4.9, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_DifferentContractLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_002") // Different contract
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // No lots for this contract, should fall back to contract-based calculation
        assertEquals(1.65, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_InactiveLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.CLOSED) // Inactive lot
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // No active lots, should fall back to contract-based calculation
        assertEquals(1.65, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_MultipleDividendsWithDifferentDates() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 30);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Should consider dividends with exDate <= 2024-01-30 and paymentDate <= 2024-01-30
        // Dividend 1: exDate 2024-01-10, paymentDate 2024-01-15 -> Valid
        // Dividend 2: exDate 2024-01-20, paymentDate 2024-01-25 -> Valid
        // Dividend 3: exDate 2024-02-10, paymentDate 2024-02-15 -> Invalid (exDate > calculation date)
        // Expected = 1000 * (1.65 + 1.50) = 3150.0
        assertEquals(3150.0, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_PaymentDateAfterCalculationDate() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 12); // Before payment date
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Dividend has exDate 2024-01-10 <= 2024-01-12, but paymentDate 2024-01-15 > 2024-01-12
        // Should not be considered
        assertEquals(0.0, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_NullPaymentDate() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // Create dividend with null paymentDate
        MarketData dividendDataWithNullPayment = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(null) // Null payment date
                                        .amount(1.65)
                                        .currency("USD")
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, dividendDataWithNullPayment, calculationDate, lots);

        // Then
        // Null paymentDate should be treated as valid (optional field)
        assertEquals(1650.0, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_NullExDate() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // Create dividend with null exDate
        MarketData dividendDataWithNullExDate = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(null) // Null exDate
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(1.65)
                                        .currency("USD")
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, dividendDataWithNullExDate, calculationDate, lots);

        // Then
        // Null exDate should be treated as invalid
        assertEquals(0.0, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_MixedLotStatuses() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build(),
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_002")
                        .contractId("EQ_SWAP_001")
                        .quantity(500.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.CLOSED) // Inactive
                        .build(),
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_003")
                        .contractId("EQ_SWAP_001")
                        .quantity(300.0)
                        .costDate(LocalDate.of(2024, 1, 8))
                        .status(null) // Null status should be treated as ACTIVE
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Only active lots: LOT_001 (1000) + LOT_003 (300) = 1300
        // Expected dividend = 1300 * 1.65 = 2145.0
        assertEquals(2145.0, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_ZeroQuantityLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(0.0) // Zero quantity
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build(),
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_002")
                        .contractId("EQ_SWAP_001")
                        .quantity(null) // Null quantity
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Total quantity = 0.0 + 0.0 = 0.0, should fall back to contract-based calculation
        assertEquals(1.65, result, 0.01);
    }

    @Test
    void testLotBasedDividendCalculation_DividendDataNotFound() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // Create market data with different underlying
        MarketData marketDataWithDifferentUnderlying = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("AAPL") // Different underlying
                        .dividends(testDividends)
                        .build())
                .build();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            dividendCalculator.calculateDividends(testContract, marketDataWithDifferentUnderlying, calculationDate, lots);
        });
    }

    @Test
    void testLotBasedDividendCalculation_ExactDateBoundaries() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15); // Exact payment date
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 10))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When
        double result = dividendCalculator.calculateDividends(testContract, testMarketData, calculationDate, lots);

        // Then
        // Dividend with exDate 2024-01-10 and paymentDate 2024-01-15 should be included
        assertEquals(1650.0, result, 0.01);
    }
}
