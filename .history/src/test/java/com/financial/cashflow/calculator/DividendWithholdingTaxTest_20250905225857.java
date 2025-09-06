package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import com.financial.cashflow.model.Dividend;
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
 * Test cases for dividend withholding tax calculations
 */
@ExtendWith(MockitoExtension.class)
class DividendWithholdingTaxTest {

    @InjectMocks
    private DividendCalculator dividendCalculator;

    private CashFlowRequest.Contract testContract;
    private MarketData testMarketData;

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
    }

    @Test
    void testGrossUpWithholdingTreatment() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Gross dividend: 1000 * $2.00 = $2000
        // Withholding tax: $2000 * 15% = $300
        // Net dividend: $2000 - $300 = $1700
        assertEquals(1700.0, result, 0.01);
    }

    @Test
    void testNetAmountWithholdingTreatment() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(1.70) // $1.70 per share (already net)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.NET_AMOUNT)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Net dividend: 1000 * $1.70 = $1700 (no additional tax deduction)
        assertEquals(1700.0, result, 0.01);
    }

    @Test
    void testNoWithholdingTreatment() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(0.0) // No withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.NO_WITHHOLDING)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Full dividend: 1000 * $2.00 = $2000 (no tax deducted)
        assertEquals(2000.0, result, 0.01);
    }

    @Test
    void testTaxCreditWithholdingTreatment() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.TAX_CREDIT)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Tax credit treatment: return gross amount as tax credit can be claimed
        // Gross dividend: 1000 * $2.00 = $2000
        assertEquals(2000.0, result, 0.01);
    }

    @Test
    void testNullWithholdingTreatmentDefaultsToGrossUp() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(null) // Null treatment
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Should default to GROSS_UP treatment
        // Gross dividend: 1000 * $2.00 = $2000
        // Withholding tax: $2000 * 15% = $300
        // Net dividend: $2000 - $300 = $1700
        assertEquals(1700.0, result, 0.01);
    }

    @Test
    void testNullWithholdingTaxRate() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(null) // Null tax rate
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // No withholding tax applied: 1000 * $2.00 = $2000
        assertEquals(2000.0, result, 0.01);
    }

    @Test
    void testZeroWithholdingTaxRate() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(0.0) // Zero tax rate
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // No withholding tax applied: 1000 * $2.00 = $2000
        assertEquals(2000.0, result, 0.01);
    }

    @Test
    void testMultipleDividendsWithDifferentWithholdingTreatments() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build(),
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 20))
                                        .paymentDate(LocalDate.of(2024, 1, 25))
                                        .amount(1.50) // $1.50 per share
                                        .currency("USD")
                                        .withholdingTaxRate(10.0) // 10% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.TAX_CREDIT)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Dividend 1: 1000 * $2.00 = $2000, GROSS_UP: $2000 - ($2000 * 15%) = $1700
        // Dividend 2: 1000 * $1.50 = $1500, TAX_CREDIT: $1500 (gross amount)
        // Total: $1700 + $1500 = $3200
        assertEquals(3200.0, result, 0.01);
    }

    @Test
    void testContractBasedDividendWithWithholdingTax() {
        // Given - No lots provided, should fall back to contract-based calculation
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Collections.emptyList();

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Contract-based calculation: notional * dividend_amount / 1000000.0
        // Gross dividend: 1000000 * $2.00 / 1000000 = $2.00
        // Withholding tax: $2.00 * 15% = $0.30
        // Net dividend: $2.00 - $0.30 = $1.70
        assertEquals(1.70, result, 0.01);
    }

    @Test
    void testHighWithholdingTaxRate() {
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

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(30.0) // 30% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        double result = dividendCalculator.calculateDividends(testContract, marketData, calculationDate, lots);

        // Then
        // Gross dividend: 1000 * $2.00 = $2000
        // Withholding tax: $2000 * 30% = $600
        // Net dividend: $2000 - $600 = $1400
        assertEquals(1400.0, result, 0.01);
    }
}
