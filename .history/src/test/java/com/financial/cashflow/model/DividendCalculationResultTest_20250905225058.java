package com.financial.cashflow.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for DividendCalculationResult
 */
class DividendCalculationResultTest {

    private WithholdingTaxInfo withholdingTaxInfo1;
    private WithholdingTaxInfo withholdingTaxInfo2;
    private DividendCalculationResult dividendCalculationResult;

    @BeforeEach
    void setUp() {
        withholdingTaxInfo1 = WithholdingTaxInfo.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .withholdingTaxAmount(300.0)
                .netDividendAmount(1700.0)
                .withholdingTreatment(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP)
                .taxJurisdiction("US")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        withholdingTaxInfo2 = WithholdingTaxInfo.builder()
                .contractId("EQ_SWAP_002")
                .underlying("AAPL")
                .currency("USD")
                .grossDividendAmount(1500.0)
                .withholdingTaxAmount(225.0)
                .netDividendAmount(1275.0)
                .withholdingTreatment(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP)
                .taxJurisdiction("US")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("CONTRACT_BASED")
                .build();
    }

    @Test
    void testGetTotalWithholdingTaxAmount_WithDetails() {
        // Given
        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(withholdingTaxInfo1, withholdingTaxInfo2);
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(2975.0) // 1700 + 1275
                .withholdingTaxDetails(withholdingTaxDetails)
                .build();

        // When
        double totalWithholdingTax = dividendCalculationResult.getTotalWithholdingTaxAmount();

        // Then
        assertEquals(525.0, totalWithholdingTax); // 300 + 225
    }

    @Test
    void testGetTotalWithholdingTaxAmount_NullDetails() {
        // Given
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(2975.0)
                .withholdingTaxDetails(null)
                .build();

        // When
        double totalWithholdingTax = dividendCalculationResult.getTotalWithholdingTaxAmount();

        // Then
        assertEquals(0.0, totalWithholdingTax);
    }

    @Test
    void testGetTotalWithholdingTaxAmount_EmptyDetails() {
        // Given
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(2975.0)
                .withholdingTaxDetails(Collections.emptyList())
                .build();

        // When
        double totalWithholdingTax = dividendCalculationResult.getTotalWithholdingTaxAmount();

        // Then
        assertEquals(0.0, totalWithholdingTax);
    }

    @Test
    void testGetTotalGrossDividendAmount_WithDetails() {
        // Given
        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(withholdingTaxInfo1, withholdingTaxInfo2);
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(2975.0) // Net amount
                .withholdingTaxDetails(withholdingTaxDetails)
                .build();

        // When
        double totalGrossDividend = dividendCalculationResult.getTotalGrossDividendAmount();

        // Then
        assertEquals(3500.0, totalGrossDividend); // 2000 + 1500
    }

    @Test
    void testGetTotalGrossDividendAmount_NullDetails() {
        // Given
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(2975.0)
                .withholdingTaxDetails(null)
                .build();

        // When
        double totalGrossDividend = dividendCalculationResult.getTotalGrossDividendAmount();

        // Then
        assertEquals(2975.0, totalGrossDividend); // Returns totalDividendAmount when details are null
    }

    @Test
    void testGetTotalGrossDividendAmount_EmptyDetails() {
        // Given
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(2975.0)
                .withholdingTaxDetails(Collections.emptyList())
                .build();

        // When
        double totalGrossDividend = dividendCalculationResult.getTotalGrossDividendAmount();

        // Then
        assertEquals(2975.0, totalGrossDividend); // Returns totalDividendAmount when details are empty
    }

    @Test
    void testBuilder() {
        // Given
        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(withholdingTaxInfo1);

        // When
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(1700.0)
                .withholdingTaxDetails(withholdingTaxDetails)
                .build();

        // Then
        assertNotNull(dividendCalculationResult);
        assertEquals(1700.0, dividendCalculationResult.getTotalDividendAmount());
        assertEquals(1, dividendCalculationResult.getWithholdingTaxDetails().size());
        assertEquals("IBM", dividendCalculationResult.getWithholdingTaxDetails().get(0).getUnderlying());
    }

    @Test
    void testNoArgsConstructor() {
        // When
        dividendCalculationResult = new DividendCalculationResult();

        // Then
        assertNotNull(dividendCalculationResult);
        assertNull(dividendCalculationResult.getTotalDividendAmount());
        assertNull(dividendCalculationResult.getWithholdingTaxDetails());
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(withholdingTaxInfo1);

        // When
        dividendCalculationResult = new DividendCalculationResult(1700.0, withholdingTaxDetails);

        // Then
        assertNotNull(dividendCalculationResult);
        assertEquals(1700.0, dividendCalculationResult.getTotalDividendAmount());
        assertEquals(1, dividendCalculationResult.getWithholdingTaxDetails().size());
    }

    @Test
    void testWithholdingTaxCalculationAccuracy() {
        // Given - Test with different withholding treatments
        WithholdingTaxInfo grossUpInfo = WithholdingTaxInfo.builder()
                .grossDividendAmount(1000.0)
                .withholdingTaxAmount(150.0) // 15% withholding
                .netDividendAmount(850.0)
                .withholdingTreatment(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP)
                .build();

        WithholdingTaxInfo taxCreditInfo = WithholdingTaxInfo.builder()
                .grossDividendAmount(1000.0)
                .withholdingTaxAmount(200.0) // 20% withholding but recoverable
                .netDividendAmount(1000.0) // Tax credit treatment returns gross amount
                .withholdingTreatment(WithholdingTaxInfo.WithholdingTreatment.TAX_CREDIT)
                .build();

        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(grossUpInfo, taxCreditInfo);
        dividendCalculationResult = DividendCalculationResult.builder()
                .totalDividendAmount(1850.0) // 850 + 1000
                .withholdingTaxDetails(withholdingTaxDetails)
                .build();

        // When
        double totalWithholdingTax = dividendCalculationResult.getTotalWithholdingTaxAmount();
        double totalGrossDividend = dividendCalculationResult.getTotalGrossDividendAmount();

        // Then
        assertEquals(350.0, totalWithholdingTax); // 150 + 200
        assertEquals(2000.0, totalGrossDividend); // 1000 + 1000
    }
}
