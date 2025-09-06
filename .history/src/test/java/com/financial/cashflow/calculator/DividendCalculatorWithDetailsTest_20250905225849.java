package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import com.financial.cashflow.model.Dividend;
import com.financial.cashflow.model.WithholdingTaxInfo;
import com.financial.cashflow.model.DividendCalculationResult;
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
 * Test cases for DividendCalculator calculateDividendsWithDetails method
 */
@ExtendWith(MockitoExtension.class)
class DividendCalculatorWithDetailsTest {

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
    void testCalculateDividendsWithDetails_LotBased_Success() {
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
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(1700.0, result.getTotalDividendAmount(), 0.01); // Net amount after withholding tax
        assertNotNull(result.getWithholdingTaxDetails());
        assertEquals(1, result.getWithholdingTaxDetails().size());

        WithholdingTaxInfo taxInfo = result.getWithholdingTaxDetails().get(0);
        assertEquals("EQ_SWAP_001", taxInfo.getContractId());
        assertEquals("IBM", taxInfo.getUnderlying());
        assertEquals("USD", taxInfo.getCurrency());
        assertEquals(2000.0, taxInfo.getGrossDividendAmount(), 0.01);
        assertEquals(15.0, taxInfo.getWithholdingTaxRate(), 0.01);
        assertEquals(300.0, taxInfo.getWithholdingTaxAmount(), 0.01);
        assertEquals(1700.0, taxInfo.getNetDividendAmount(), 0.01);
        assertEquals(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP, taxInfo.getWithholdingTreatment());
        assertEquals("US", taxInfo.getTaxJurisdiction());
        assertEquals("LOT_BASED", taxInfo.getCalculationType());
        assertNotNull(taxInfo.getTaxUtilityReference());
        assertEquals(calculationDate, taxInfo.getCalculationDate());
    }

    @Test
    void testCalculateDividendsWithDetails_ContractBased_Success() {
        // Given - No lots provided, should use contract-based calculation
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Collections.emptyList();

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(1.70, result.getTotalDividendAmount(), 0.01); // Contract-based: notional * dividend / 1000000
        assertNotNull(result.getWithholdingTaxDetails());
        assertEquals(1, result.getWithholdingTaxDetails().size());

        WithholdingTaxInfo taxInfo = result.getWithholdingTaxDetails().get(0);
        assertEquals("EQ_SWAP_001", taxInfo.getContractId());
        assertEquals("CONTRACT_BASED", taxInfo.getCalculationType());
        assertEquals(2.0, taxInfo.getGrossDividendAmount(), 0.01); // Contract-based gross amount
        assertEquals(0.30, taxInfo.getWithholdingTaxAmount(), 0.01); // 15% of 2.0
        assertEquals(1.70, taxInfo.getNetDividendAmount(), 0.01);
    }

    @Test
    void testCalculateDividendsWithDetails_MultipleDividends() {
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
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00) // $2.00 per share
                                        .currency("USD")
                                        .withholdingTaxRate(15.0) // 15% withholding tax
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build(),
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 20))
                                        .paymentDate(LocalDate.of(2024, 1, 25))
                                        .amount(1.50) // $1.50 per share
                                        .currency("USD")
                                        .withholdingTaxRate(10.0) // 10% withholding tax
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.TAX_CREDIT)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(3200.0, result.getTotalDividendAmount(), 0.01); // 1700 + 1500
        assertNotNull(result.getWithholdingTaxDetails());
        assertEquals(2, result.getWithholdingTaxDetails().size());

        // Verify first dividend details
        WithholdingTaxInfo taxInfo1 = result.getWithholdingTaxDetails().get(0);
        assertEquals(2000.0, taxInfo1.getGrossDividendAmount(), 0.01);
        assertEquals(300.0, taxInfo1.getWithholdingTaxAmount(), 0.01);
        assertEquals(1700.0, taxInfo1.getNetDividendAmount(), 0.01);
        assertEquals(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP, taxInfo1.getWithholdingTreatment());

        // Verify second dividend details
        WithholdingTaxInfo taxInfo2 = result.getWithholdingTaxDetails().get(1);
        assertEquals(1500.0, taxInfo2.getGrossDividendAmount(), 0.01);
        assertEquals(0.0, taxInfo2.getWithholdingTaxAmount(), 0.01); // TAX_CREDIT treatment doesn't deduct tax
        assertEquals(1500.0, taxInfo2.getNetDividendAmount(), 0.01); // TAX_CREDIT returns gross amount
        assertEquals(WithholdingTaxInfo.WithholdingTreatment.TAX_CREDIT, taxInfo2.getWithholdingTreatment());
    }

    @Test
    void testCalculateDividendsWithDetails_DifferentTaxJurisdictions() {
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

        // Test different currencies for tax jurisdiction detection
        CashFlowRequest.Contract eurContract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_EUR")
                .underlying("SAP")
                .currency("EUR")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("SAP")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(1.50) // €1.50 per share
                                        .currency("EUR")
                                        .withholdingTaxRate(20.0) // 20% withholding tax
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                eurContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getWithholdingTaxDetails().size()); // No dividends match the calculation date
    }

    @Test
    void testCalculateDividendsWithDetails_NoValidLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 2, 1)) // Future cost date - not valid
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getTotalDividendAmount(), 0.01); // No valid lots, no dividends calculated
        assertNotNull(result.getWithholdingTaxDetails());
        assertEquals(0, result.getWithholdingTaxDetails().size());
    }

    @Test
    void testCalculateDividendsWithDetails_NullLots() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);

        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, null);

        // Then
        assertNotNull(result);
        assertEquals(1.70, result.getTotalDividendAmount(), 0.01); // Falls back to contract-based
        assertNotNull(result.getWithholdingTaxDetails());
        assertEquals(1, result.getWithholdingTaxDetails().size());
        assertEquals("CONTRACT_BASED", result.getWithholdingTaxDetails().get(0).getCalculationType());
    }

    @Test
    void testCalculateDividendsWithDetails_TaxUtilityReferenceGeneration() {
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
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getWithholdingTaxDetails().size());

        WithholdingTaxInfo taxInfo = result.getWithholdingTaxDetails().get(0);
        assertNotNull(taxInfo.getTaxUtilityReference());
        assertEquals("TAX_EQ_SWAP_001_20240110", taxInfo.getTaxUtilityReference());
    }

    @Test
    void testCalculateDividendsWithDetails_ExceptionHandling() {
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

        // Market data without dividend data for the underlying
        MarketData marketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("UNKNOWN") // Different symbol
                        .dividends(Collections.emptyList())
                        .build())
                .build();

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            dividendCalculator.calculateDividendsWithDetails(testContract, marketData, calculationDate, lots));
    }

    @Test
    void testCalculateDividendsWithDetails_AllWithholdingTreatments() {
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
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.GROSS_UP)
                                        .build(),
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 20))
                                        .paymentDate(LocalDate.of(2024, 1, 25))
                                        .amount(1.50)
                                        .currency("USD")
                                        .withholdingTaxRate(10.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.NET_AMOUNT)
                                        .build(),
                                MarketData.DividendData.Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 30))
                                        .paymentDate(LocalDate.of(2024, 2, 5))
                                        .amount(1.00)
                                        .currency("USD")
                                        .withholdingTaxRate(0.0)
                                        .withholdingTreatment(MarketData.DividendData.Dividend.WithholdingTreatment.NO_WITHHOLDING)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, marketData, calculationDate, lots);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getWithholdingTaxDetails().size()); // Only first dividend matches the calculation date

        // Verify GROSS_UP treatment
        WithholdingTaxInfo grossUpInfo = result.getWithholdingTaxDetails().get(0);
        assertEquals(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP, grossUpInfo.getWithholdingTreatment());
        assertEquals(2000.0, grossUpInfo.getGrossDividendAmount(), 0.01);
        assertEquals(300.0, grossUpInfo.getWithholdingTaxAmount(), 0.01);
        assertEquals(1700.0, grossUpInfo.getNetDividendAmount(), 0.01);

        // Only first dividend (GROSS_UP) matches the calculation date
    }
}
