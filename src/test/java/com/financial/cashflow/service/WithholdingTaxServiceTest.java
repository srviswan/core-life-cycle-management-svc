package com.financial.cashflow.service;

import com.financial.cashflow.entity.WithholdingTaxEntity;
import com.financial.cashflow.model.WithholdingTaxInfo;
import com.financial.cashflow.repository.WithholdingTaxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test cases for WithholdingTaxService
 */
@ExtendWith(MockitoExtension.class)
class WithholdingTaxServiceTest {

    @Mock
    private WithholdingTaxRepository withholdingTaxRepository;

    @InjectMocks
    private WithholdingTaxService withholdingTaxService;

    private WithholdingTaxInfo sampleWithholdingTaxInfo;
    private WithholdingTaxEntity sampleWithholdingTaxEntity;

    @BeforeEach
    void setUp() {
        sampleWithholdingTaxInfo = WithholdingTaxInfo.builder()
                .contractId("EQ_SWAP_001")
                .lotId("LOT_001")
                .underlying("IBM")
                .currency("USD")
                .exDate(LocalDate.of(2024, 1, 10))
                .paymentDate(LocalDate.of(2024, 1, 15))
                .grossDividendAmount(2000.0)
                .withholdingTaxRate(15.0)
                .withholdingTaxAmount(300.0)
                .netDividendAmount(1700.0)
                .withholdingTreatment(WithholdingTaxInfo.WithholdingTreatment.GROSS_UP)
                .taxJurisdiction("US")
                .taxUtilityReference("TAX_EQ_SWAP_001_20240110")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        sampleWithholdingTaxEntity = WithholdingTaxEntity.builder()
                .id(1L)
                .contractId("EQ_SWAP_001")
                .lotId("LOT_001")
                .underlying("IBM")
                .currency("USD")
                .exDate(LocalDate.of(2024, 1, 10))
                .paymentDate(LocalDate.of(2024, 1, 15))
                .grossDividendAmount(2000.0)
                .withholdingTaxRate(15.0)
                .withholdingTaxAmount(300.0)
                .netDividendAmount(1700.0)
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.GROSS_UP)
                .taxJurisdiction("US")
                .taxUtilityReference("TAX_EQ_SWAP_001_20240110")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();
    }

    @Test
    void testSaveWithholdingTaxDetails_Success() {
        // Given
        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(sampleWithholdingTaxInfo);
        when(withholdingTaxRepository.saveAll(any())).thenReturn(Arrays.asList(sampleWithholdingTaxEntity));

        // When
        withholdingTaxService.saveWithholdingTaxDetails(withholdingTaxDetails);

        // Then
        verify(withholdingTaxRepository, times(1)).saveAll(any());
    }

    @Test
    void testSaveWithholdingTaxDetails_EmptyList() {
        // When
        withholdingTaxService.saveWithholdingTaxDetails(Collections.emptyList());

        // Then
        verify(withholdingTaxRepository, never()).saveAll(any());
    }

    @Test
    void testSaveWithholdingTaxDetails_NullList() {
        // When
        withholdingTaxService.saveWithholdingTaxDetails(null);

        // Then
        verify(withholdingTaxRepository, never()).saveAll(any());
    }

    @Test
    void testSaveWithholdingTaxDetails_Exception() {
        // Given
        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(sampleWithholdingTaxInfo);
        when(withholdingTaxRepository.saveAll(any())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            withholdingTaxService.saveWithholdingTaxDetails(withholdingTaxDetails));
    }

    @Test
    void testGetWithholdingTaxByContract_Success() {
        // Given
        String contractId = "EQ_SWAP_001";
        when(withholdingTaxRepository.findByContractId(contractId))
                .thenReturn(Arrays.asList(sampleWithholdingTaxEntity));

        // When
        List<WithholdingTaxInfo> result = withholdingTaxService.getWithholdingTaxByContract(contractId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(contractId, result.get(0).getContractId());
        assertEquals("IBM", result.get(0).getUnderlying());
        assertEquals(2000.0, result.get(0).getGrossDividendAmount());
        assertEquals(300.0, result.get(0).getWithholdingTaxAmount());
        assertEquals(1700.0, result.get(0).getNetDividendAmount());
    }

    @Test
    void testGetWithholdingTaxByContractAndDateRange_Success() {
        // Given
        String contractId = "EQ_SWAP_001";
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        when(withholdingTaxRepository.findByContractIdAndDateRange(contractId, fromDate, toDate))
                .thenReturn(Arrays.asList(sampleWithholdingTaxEntity));

        // When
        List<WithholdingTaxInfo> result = withholdingTaxService.getWithholdingTaxByContractAndDateRange(
                contractId, fromDate, toDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(contractId, result.get(0).getContractId());
        verify(withholdingTaxRepository, times(1)).findByContractIdAndDateRange(contractId, fromDate, toDate);
    }

    @Test
    void testGetWithholdingTaxForTaxUtilityReporting_Success() {
        // Given
        String jurisdiction = "US";
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        when(withholdingTaxRepository.findForTaxUtilityReporting(jurisdiction, fromDate, toDate))
                .thenReturn(Arrays.asList(sampleWithholdingTaxEntity));

        // When
        List<WithholdingTaxInfo> result = withholdingTaxService.getWithholdingTaxForTaxUtilityReporting(
                jurisdiction, fromDate, toDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("US", result.get(0).getTaxJurisdiction());
        verify(withholdingTaxRepository, times(1)).findForTaxUtilityReporting(jurisdiction, fromDate, toDate);
    }

    @Test
    void testGetTotalWithholdingTaxByContract_Success() {
        // Given
        String contractId = "EQ_SWAP_001";
        when(withholdingTaxRepository.getTotalWithholdingTaxByContract(contractId))
                .thenReturn(300.0);

        // When
        double result = withholdingTaxService.getTotalWithholdingTaxByContract(contractId);

        // Then
        assertEquals(300.0, result);
        verify(withholdingTaxRepository, times(1)).getTotalWithholdingTaxByContract(contractId);
    }

    @Test
    void testGetTotalWithholdingTaxByContract_NullResult() {
        // Given
        String contractId = "EQ_SWAP_001";
        when(withholdingTaxRepository.getTotalWithholdingTaxByContract(contractId))
                .thenReturn(null);

        // When
        double result = withholdingTaxService.getTotalWithholdingTaxByContract(contractId);

        // Then
        assertEquals(0.0, result);
    }

    @Test
    void testGetTotalWithholdingTaxByJurisdiction_Success() {
        // Given
        String jurisdiction = "US";
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        when(withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(jurisdiction, fromDate, toDate))
                .thenReturn(1500.0);

        // When
        double result = withholdingTaxService.getTotalWithholdingTaxByJurisdiction(jurisdiction, fromDate, toDate);

        // Then
        assertEquals(1500.0, result);
        verify(withholdingTaxRepository, times(1)).getTotalWithholdingTaxByJurisdiction(jurisdiction, fromDate, toDate);
    }

    @Test
    void testGetTotalWithholdingTaxByJurisdiction_NullResult() {
        // Given
        String jurisdiction = "US";
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        when(withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(jurisdiction, fromDate, toDate))
                .thenReturn(null);

        // When
        double result = withholdingTaxService.getTotalWithholdingTaxByJurisdiction(jurisdiction, fromDate, toDate);

        // Then
        assertEquals(0.0, result);
    }

    @Test
    void testGetWithholdingTaxByContract_Exception() {
        // Given
        String contractId = "EQ_SWAP_001";
        when(withholdingTaxRepository.findByContractId(contractId))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            withholdingTaxService.getWithholdingTaxByContract(contractId));
    }

    @Test
    void testMultipleWithholdingTaxDetails() {
        // Given
        WithholdingTaxInfo info1 = WithholdingTaxInfo.builder()
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

        WithholdingTaxInfo info2 = WithholdingTaxInfo.builder()
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

        List<WithholdingTaxInfo> withholdingTaxDetails = Arrays.asList(info1, info2);
        when(withholdingTaxRepository.saveAll(any())).thenReturn(Arrays.asList(sampleWithholdingTaxEntity));

        // When
        withholdingTaxService.saveWithholdingTaxDetails(withholdingTaxDetails);

        // Then
        verify(withholdingTaxRepository, times(1)).saveAll(any());
    }
}
