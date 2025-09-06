package com.financial.cashflow.integration;

import com.financial.cashflow.calculator.DividendCalculator;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import com.financial.cashflow.model.Dividend;
import com.financial.cashflow.model.WithholdingTaxInfo;
import com.financial.cashflow.model.DividendCalculationResult;
import com.financial.cashflow.service.WithholdingTaxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for withholding tax tracking functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WithholdingTaxIntegrationTest {

    @Autowired
    private DividendCalculator dividendCalculator;

    @Autowired
    private WithholdingTaxService withholdingTaxService;

    private CashFlowRequest.Contract testContract;
    private MarketData testMarketData;

    @BeforeEach
    void setUp() {
        // Create test contract
        testContract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_INTEGRATION_001")
                .underlying("IBM")
                .index("LIBOR_3M")
                .notionalAmount(1000000.0)
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // Create test market data
        testMarketData = MarketData.builder()
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
                                        .exDate(LocalDate.of(2024, 3, 10))
                                        .paymentDate(LocalDate.of(2024, 3, 15))
                                        .amount(1.50) // $1.50 per share
                                        .currency("USD")
                                        .withholdingTaxRate(10.0) // 10% withholding tax
                                        .withholdingTreatment(Dividend.WithholdingTreatment.TAX_CREDIT)
                                        .build()
                        ))
                        .build())
                .timestamp(java.time.LocalDateTime.now())
                .source("INTEGRATION_TEST")
                .isValid(true)
                .build();
    }

    @Test
    void testEndToEndWithholdingTaxTracking_LotBased() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 3, 20);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_INTEGRATION_001")
                        .contractId("EQ_SWAP_INTEGRATION_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build(),
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_INTEGRATION_002")
                        .contractId("EQ_SWAP_INTEGRATION_001")
                        .quantity(500.0)
                        .costDate(LocalDate.of(2024, 2, 1))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When - Calculate dividends with details
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, testMarketData, calculationDate, lots);

        // Then - Verify calculation results
        assertNotNull(result);
        assertEquals(2, result.getWithholdingTaxDetails().size());

        // Verify first dividend (Q1)
        WithholdingTaxInfo q1TaxInfo = result.getWithholdingTaxDetails().get(0);
        assertEquals("EQ_SWAP_INTEGRATION_001", q1TaxInfo.getContractId());
        assertEquals("IBM", q1TaxInfo.getUnderlying());
        assertEquals(3000.0, q1TaxInfo.getGrossDividendAmount(), 0.01); // 1500 shares * $2.00
        assertEquals(450.0, q1TaxInfo.getWithholdingTaxAmount(), 0.01); // 15% of $3000
        assertEquals(2550.0, q1TaxInfo.getNetDividendAmount(), 0.01);
        assertEquals(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP, q1TaxInfo.getWithholdingTreatment());
        assertEquals("US", q1TaxInfo.getTaxJurisdiction());
        assertEquals("LOT_BASED", q1TaxInfo.getCalculationType());

        // Verify second dividend (Q2)
        WithholdingTaxInfo q2TaxInfo = result.getWithholdingTaxDetails().get(1);
        assertEquals(2250.0, q2TaxInfo.getGrossDividendAmount(), 0.01); // 1500 shares * $1.50
        assertEquals(225.0, q2TaxInfo.getWithholdingTaxAmount(), 0.01); // 10% of $2250
        assertEquals(2250.0, q2TaxInfo.getNetDividendAmount(), 0.01); // TAX_CREDIT returns gross
        assertEquals(WithholdingTaxInfo.WithholdingTreatment.TAX_CREDIT, q2TaxInfo.getWithholdingTreatment());

        // When - Save withholding tax details
        withholdingTaxService.saveWithholdingTaxDetails(result.getWithholdingTaxDetails());

        // Then - Verify data was saved and can be retrieved
        List<WithholdingTaxInfo> savedTaxDetails = withholdingTaxService.getWithholdingTaxByContract(
                "EQ_SWAP_INTEGRATION_001");

        assertNotNull(savedTaxDetails);
        assertEquals(2, savedTaxDetails.size());

        // Verify first saved record
        WithholdingTaxInfo savedQ1 = savedTaxDetails.get(0);
        assertEquals("EQ_SWAP_INTEGRATION_001", savedQ1.getContractId());
        assertEquals(3000.0, savedQ1.getGrossDividendAmount(), 0.01);
        assertEquals(450.0, savedQ1.getWithholdingTaxAmount(), 0.01);
        assertEquals(2550.0, savedQ1.getNetDividendAmount(), 0.01);

        // Verify second saved record
        WithholdingTaxInfo savedQ2 = savedTaxDetails.get(1);
        assertEquals(2250.0, savedQ2.getGrossDividendAmount(), 0.01);
        assertEquals(225.0, savedQ2.getWithholdingTaxAmount(), 0.01);
        assertEquals(2250.0, savedQ2.getNetDividendAmount(), 0.01);
    }

    @Test
    void testEndToEndWithholdingTaxTracking_ContractBased() {
        // Given - No lots provided, should use contract-based calculation
        LocalDate calculationDate = LocalDate.of(2024, 3, 20);
        List<CashFlowRequest.Lot> lots = Arrays.asList(); // Empty lots

        // When - Calculate dividends with details
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, testMarketData, calculationDate, lots);

        // Then - Verify calculation results
        assertNotNull(result);
        assertEquals(2, result.getWithholdingTaxDetails().size());

        // Verify first dividend (contract-based)
        WithholdingTaxInfo q1TaxInfo = result.getWithholdingTaxDetails().get(0);
        assertEquals("CONTRACT_BASED", q1TaxInfo.getCalculationType());
        assertEquals(2.0, q1TaxInfo.getGrossDividendAmount(), 0.01); // Contract-based: notional * dividend / 1000000
        assertEquals(0.30, q1TaxInfo.getWithholdingTaxAmount(), 0.01); // 15% of $2.00
        assertEquals(1.70, q1TaxInfo.getNetDividendAmount(), 0.01);

        // Verify second dividend (contract-based)
        WithholdingTaxInfo q2TaxInfo = result.getWithholdingTaxDetails().get(1);
        assertEquals("CONTRACT_BASED", q2TaxInfo.getCalculationType());
        assertEquals(1.50, q2TaxInfo.getGrossDividendAmount(), 0.01);
        assertEquals(0.15, q2TaxInfo.getWithholdingTaxAmount(), 0.01); // 10% of $1.50
        assertEquals(1.50, q2TaxInfo.getNetDividendAmount(), 0.01); // TAX_CREDIT returns gross

        // When - Save withholding tax details
        withholdingTaxService.saveWithholdingTaxDetails(result.getWithholdingTaxDetails());

        // Then - Verify data was saved
        List<WithholdingTaxInfo> savedTaxDetails = withholdingTaxService.getWithholdingTaxByContract(
                "EQ_SWAP_INTEGRATION_001");

        assertNotNull(savedTaxDetails);
        assertEquals(2, savedTaxDetails.size());
        assertEquals("CONTRACT_BASED", savedTaxDetails.get(0).getCalculationType());
        assertEquals("CONTRACT_BASED", savedTaxDetails.get(1).getCalculationType());
    }

    @Test
    void testTaxUtilityReportingByJurisdiction() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 3, 20);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_INTEGRATION_001")
                        .contractId("EQ_SWAP_INTEGRATION_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build(),
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_INTEGRATION_002")
                        .contractId("EQ_SWAP_INTEGRATION_001")
                        .quantity(500.0)
                        .costDate(LocalDate.of(2024, 2, 1))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When - Calculate and save withholding tax details
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, testMarketData, calculationDate, lots);
        withholdingTaxService.saveWithholdingTaxDetails(result.getWithholdingTaxDetails());

        // Then - Test tax utility reporting queries
        List<WithholdingTaxInfo> usTaxDetails = withholdingTaxService.getWithholdingTaxForTaxUtilityReporting(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertNotNull(usTaxDetails);
        assertEquals(2, usTaxDetails.size()); // Both dividends are USD (US jurisdiction)

        // Verify all records are for US jurisdiction
        for (WithholdingTaxInfo taxInfo : usTaxDetails) {
            assertEquals("US", taxInfo.getTaxJurisdiction());
            assertTrue(taxInfo.getWithholdingTaxAmount() > 0); // Only records with withholding tax
        }

        // Test total withholding tax calculation
        double totalUSTax = withholdingTaxService.getTotalWithholdingTaxByJurisdiction(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        assertEquals(675.0, totalUSTax, 0.01); // 450 + 225
    }

    @Test
    void testTaxUtilityReportingByDateRange() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 3, 20);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_INTEGRATION_001")
                        .contractId("EQ_SWAP_INTEGRATION_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When - Calculate and save withholding tax details
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, testMarketData, calculationDate, lots);
        withholdingTaxService.saveWithholdingTaxDetails(result.getWithholdingTaxDetails());

        // Then - Test date range queries
        List<WithholdingTaxInfo> q1TaxDetails = withholdingTaxService.getWithholdingTaxByContractAndDateRange(
                "EQ_SWAP_INTEGRATION_001", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));

        assertNotNull(q1TaxDetails);
        assertEquals(2, q1TaxDetails.size()); // Both dividends fall within Q1-Q2 range

        // Test Q1 only
        List<WithholdingTaxInfo> q1OnlyTaxDetails = withholdingTaxService.getWithholdingTaxByContractAndDateRange(
                "EQ_SWAP_INTEGRATION_001", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 29));

        assertNotNull(q1OnlyTaxDetails);
        assertEquals(1, q1OnlyTaxDetails.size()); // Only first dividend falls within Q1
        assertEquals(LocalDate.of(2024, 1, 10), q1OnlyTaxDetails.get(0).getExDate());
    }

    @Test
    void testMultipleContractsWithholdingTaxTracking() {
        // Given - Create multiple contracts with different currencies
        CashFlowRequest.Contract usdContract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_USD_001")
                .underlying("IBM")
                .currency("USD")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        CashFlowRequest.Contract eurContract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_EUR_001")
                .underlying("SAP")
                .currency("EUR")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        MarketData multiCurrencyMarketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("IBM")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(2.00)
                                        .currency("USD")
                                        .withholdingTaxRate(15.0)
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        MarketData sapMarketData = MarketData.builder()
                .dividends(MarketData.DividendData.builder()
                        .symbol("SAP")
                        .dividends(Arrays.asList(
                                Dividend.builder()
                                        .exDate(LocalDate.of(2024, 1, 10))
                                        .paymentDate(LocalDate.of(2024, 1, 15))
                                        .amount(1.50)
                                        .currency("EUR")
                                        .withholdingTaxRate(20.0)
                                        .withholdingTreatment(Dividend.WithholdingTreatment.GROSS_UP)
                                        .build()
                        ))
                        .build())
                .build();

        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_USD_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When - Calculate dividends for both contracts
        DividendCalculationResult usdResult = dividendCalculator.calculateDividendsWithDetails(
                usdContract, multiCurrencyMarketData, calculationDate, lots);

        DividendCalculationResult eurResult = dividendCalculator.calculateDividendsWithDetails(
                eurContract, sapMarketData, calculationDate, lots);

        // Save both results
        withholdingTaxService.saveWithholdingTaxDetails(usdResult.getWithholdingTaxDetails());
        withholdingTaxService.saveWithholdingTaxDetails(eurResult.getWithholdingTaxDetails());

        // Then - Verify jurisdiction-based reporting
        List<WithholdingTaxInfo> usTaxDetails = withholdingTaxService.getWithholdingTaxForTaxUtilityReporting(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        List<WithholdingTaxInfo> euTaxDetails = withholdingTaxService.getWithholdingTaxForTaxUtilityReporting(
                "EU", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertNotNull(usTaxDetails);
        assertEquals(1, usTaxDetails.size());
        assertEquals("US", usTaxDetails.get(0).getTaxJurisdiction());
        assertEquals("USD", usTaxDetails.get(0).getCurrency());

        assertNotNull(euTaxDetails);
        assertEquals(1, euTaxDetails.size());
        assertEquals("EU", euTaxDetails.get(0).getTaxJurisdiction());
        assertEquals("EUR", euTaxDetails.get(0).getCurrency());

        // Verify total withholding tax by jurisdiction
        double totalUSTax = withholdingTaxService.getTotalWithholdingTaxByJurisdiction(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        double totalEUTax = withholdingTaxService.getTotalWithholdingTaxByJurisdiction(
                "EU", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertEquals(300.0, totalUSTax, 0.01); // 15% of $2000
        assertEquals(300.0, totalEUTax, 0.01); // 20% of €1500
    }

    @Test
    void testTaxUtilityReferenceUniqueness() {
        // Given
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = Arrays.asList(
                CashFlowRequest.Lot.builder()
                        .lotId("LOT_001")
                        .contractId("EQ_SWAP_INTEGRATION_001")
                        .quantity(1000.0)
                        .costDate(LocalDate.of(2024, 1, 5))
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .build()
        );

        // When - Calculate dividends with details
        DividendCalculationResult result = dividendCalculator.calculateDividendsWithDetails(
                testContract, testMarketData, calculationDate, lots);

        // Then - Verify tax utility references are unique and properly formatted
        assertNotNull(result.getWithholdingTaxDetails());
        assertEquals(2, result.getWithholdingTaxDetails().size());

        WithholdingTaxInfo taxInfo1 = result.getWithholdingTaxDetails().get(0);
        WithholdingTaxInfo taxInfo2 = result.getWithholdingTaxDetails().get(1);

        assertNotNull(taxInfo1.getTaxUtilityReference());
        assertNotNull(taxInfo2.getTaxUtilityReference());
        assertNotEquals(taxInfo1.getTaxUtilityReference(), taxInfo2.getTaxUtilityReference());

        // Verify reference format: TAX_{contractId}_{exDate}
        assertTrue(taxInfo1.getTaxUtilityReference().startsWith("TAX_EQ_SWAP_INTEGRATION_001_"));
        assertTrue(taxInfo2.getTaxUtilityReference().startsWith("TAX_EQ_SWAP_INTEGRATION_001_"));

        // Verify reference contains the ex-date
        assertTrue(taxInfo1.getTaxUtilityReference().contains("20240110"));
        assertTrue(taxInfo2.getTaxUtilityReference().contains("20240310"));
    }
}
