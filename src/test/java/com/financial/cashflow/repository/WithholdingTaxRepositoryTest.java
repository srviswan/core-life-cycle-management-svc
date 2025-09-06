package com.financial.cashflow.repository;

import com.financial.cashflow.entity.WithholdingTaxEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for WithholdingTaxRepository queries
 */
@DataJpaTest
@ActiveProfiles("test")
class WithholdingTaxRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WithholdingTaxRepository withholdingTaxRepository;

    private WithholdingTaxEntity entity1;
    private WithholdingTaxEntity entity2;
    private WithholdingTaxEntity entity3;

    @BeforeEach
    void setUp() {
        // Create test entities
        entity1 = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_REPO_001")
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
                .taxUtilityReference("TAX_EQ_SWAP_REPO_001_20240110")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        entity2 = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_REPO_001")
                .lotId("LOT_002")
                .underlying("IBM")
                .currency("USD")
                .exDate(LocalDate.of(2024, 3, 10))
                .paymentDate(LocalDate.of(2024, 3, 15))
                .grossDividendAmount(1500.0)
                .withholdingTaxRate(10.0)
                .withholdingTaxAmount(150.0)
                .netDividendAmount(1350.0)
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.TAX_CREDIT)
                .taxJurisdiction("US")
                .taxUtilityReference("TAX_EQ_SWAP_REPO_001_20240310")
                .calculationDate(LocalDate.of(2024, 3, 15))
                .calculationType("CONTRACT_BASED")
                .build();

        entity3 = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_REPO_002")
                .underlying("AAPL")
                .currency("EUR")
                .exDate(LocalDate.of(2024, 2, 10))
                .paymentDate(LocalDate.of(2024, 2, 15))
                .grossDividendAmount(1000.0)
                .withholdingTaxRate(20.0)
                .withholdingTaxAmount(200.0)
                .netDividendAmount(800.0)
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.GROSS_UP)
                .taxJurisdiction("EU")
                .taxUtilityReference("TAX_EQ_SWAP_REPO_002_20240210")
                .calculationDate(LocalDate.of(2024, 2, 15))
                .calculationType("LOT_BASED")
                .build();

        // Persist entities
        entityManager.persistAndFlush(entity1);
        entityManager.persistAndFlush(entity2);
        entityManager.persistAndFlush(entity3);
    }

    @Test
    void testFindByContractId() {
        // When
        List<WithholdingTaxEntity> result = withholdingTaxRepository.findByContractId("EQ_SWAP_REPO_001");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify both entities belong to the same contract
        for (WithholdingTaxEntity entity : result) {
            assertEquals("EQ_SWAP_REPO_001", entity.getContractId());
        }
    }

    @Test
    void testFindByContractIdAndDateRange() {
        // When
        List<WithholdingTaxEntity> result = withholdingTaxRepository.findByContractIdAndDateRange(
                "EQ_SWAP_REPO_001", 
                LocalDate.of(2024, 1, 1), 
                LocalDate.of(2024, 2, 29));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EQ_SWAP_REPO_001", result.get(0).getContractId());
        assertEquals(LocalDate.of(2024, 1, 15), result.get(0).getCalculationDate());
    }

    @Test
    void testFindByContractIdAndDateRange_NoResults() {
        // When
        List<WithholdingTaxEntity> result = withholdingTaxRepository.findByContractIdAndDateRange(
                "EQ_SWAP_REPO_001", 
                LocalDate.of(2024, 6, 1), 
                LocalDate.of(2024, 12, 31));

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByTaxUtilityReference() {
        // When
        List<WithholdingTaxEntity> result = withholdingTaxRepository.findByTaxUtilityReference(
                "TAX_EQ_SWAP_REPO_001_20240110");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TAX_EQ_SWAP_REPO_001_20240110", result.get(0).getTaxUtilityReference());
        assertEquals("EQ_SWAP_REPO_001", result.get(0).getContractId());
    }

    @Test
    void testFindByTaxJurisdiction() {
        // When
        List<WithholdingTaxEntity> usResult = withholdingTaxRepository.findByTaxJurisdiction("US");
        List<WithholdingTaxEntity> euResult = withholdingTaxRepository.findByTaxJurisdiction("EU");

        // Then
        assertNotNull(usResult);
        assertEquals(2, usResult.size());
        for (WithholdingTaxEntity entity : usResult) {
            assertEquals("US", entity.getTaxJurisdiction());
        }

        assertNotNull(euResult);
        assertEquals(1, euResult.size());
        assertEquals("EU", euResult.get(0).getTaxJurisdiction());
    }

    @Test
    void testFindByCalculationType() {
        // When
        List<WithholdingTaxEntity> lotBasedResult = withholdingTaxRepository.findByCalculationType("LOT_BASED");
        List<WithholdingTaxEntity> contractBasedResult = withholdingTaxRepository.findByCalculationType("CONTRACT_BASED");

        // Then
        assertNotNull(lotBasedResult);
        assertEquals(2, lotBasedResult.size());
        for (WithholdingTaxEntity entity : lotBasedResult) {
            assertEquals("LOT_BASED", entity.getCalculationType());
        }

        assertNotNull(contractBasedResult);
        assertEquals(1, contractBasedResult.size());
        assertEquals("CONTRACT_BASED", contractBasedResult.get(0).getCalculationType());
    }

    @Test
    void testFindForTaxUtilityReporting() {
        // When
        List<WithholdingTaxEntity> usResult = withholdingTaxRepository.findForTaxUtilityReporting(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        List<WithholdingTaxEntity> euResult = withholdingTaxRepository.findForTaxUtilityReporting(
                "EU", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        // Then
        assertNotNull(usResult);
        assertEquals(2, usResult.size());
        for (WithholdingTaxEntity entity : usResult) {
            assertEquals("US", entity.getTaxJurisdiction());
            assertTrue(entity.getWithholdingTaxAmount() > 0); // Only records with withholding tax
        }

        assertNotNull(euResult);
        assertEquals(1, euResult.size());
        assertEquals("EU", euResult.get(0).getTaxJurisdiction());
        assertTrue(euResult.get(0).getWithholdingTaxAmount() > 0);
    }

    @Test
    void testFindForTaxUtilityReporting_DateRange() {
        // When
        List<WithholdingTaxEntity> q1Result = withholdingTaxRepository.findForTaxUtilityReporting(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));

        List<WithholdingTaxEntity> q2Result = withholdingTaxRepository.findForTaxUtilityReporting(
                "US", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 6, 30));

        // Then
        assertNotNull(q1Result);
        assertEquals(2, q1Result.size()); // Both US entities fall within Q1-Q2

        assertNotNull(q2Result);
        assertTrue(q2Result.isEmpty()); // No entities in Q2
    }

    @Test
    void testGetTotalWithholdingTaxByContract() {
        // When
        Double totalTax1 = withholdingTaxRepository.getTotalWithholdingTaxByContract("EQ_SWAP_REPO_001");
        Double totalTax2 = withholdingTaxRepository.getTotalWithholdingTaxByContract("EQ_SWAP_REPO_002");
        Double totalTax3 = withholdingTaxRepository.getTotalWithholdingTaxByContract("NON_EXISTENT");

        // Then
        assertNotNull(totalTax1);
        assertEquals(450.0, totalTax1, 0.01); // 300 + 150

        assertNotNull(totalTax2);
        assertEquals(200.0, totalTax2, 0.01);

        assertNull(totalTax3); // Non-existent contract should return null
    }

    @Test
    void testGetTotalWithholdingTaxByJurisdiction() {
        // When
        Double totalUSTax = withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        Double totalEUTax = withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(
                "EU", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        Double totalUnknownTax = withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(
                "UNKNOWN", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        // Then
        assertNotNull(totalUSTax);
        assertEquals(450.0, totalUSTax, 0.01); // 300 + 150

        assertNotNull(totalEUTax);
        assertEquals(200.0, totalEUTax, 0.01);

        assertNull(totalUnknownTax); // Unknown jurisdiction should return null
    }

    @Test
    void testGetTotalWithholdingTaxByJurisdiction_DateRange() {
        // When
        Double q1USTax = withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));

        Double q2USTax = withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(
                "US", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 6, 30));

        // Then
        assertNotNull(q1USTax);
        assertEquals(450.0, q1USTax, 0.01); // Both US entities fall within Q1-Q2

        assertNull(q2USTax); // No entities in Q2
    }

    @Test
    void testRepositoryWithNoWithholdingTax() {
        // Given - Create entity with no withholding tax
        WithholdingTaxEntity noTaxEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_NO_TAX_001")
                .underlying("MSFT")
                .currency("USD")
                .grossDividendAmount(1000.0)
                .withholdingTaxAmount(0.0)
                .netDividendAmount(1000.0)
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.NO_WITHHOLDING)
                .taxJurisdiction("US")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        entityManager.persistAndFlush(noTaxEntity);

        // When
        List<WithholdingTaxEntity> taxUtilityResult = withholdingTaxRepository.findForTaxUtilityReporting(
                "US", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        Double totalTax = withholdingTaxRepository.getTotalWithholdingTaxByContract("EQ_SWAP_NO_TAX_001");

        // Then
        // Tax utility reporting only includes entities with withholding tax > 0
        assertEquals(2, taxUtilityResult.size()); // Only the 2 original entities with tax > 0

        assertNotNull(totalTax);
        assertEquals(0.0, totalTax, 0.01); // No withholding tax
    }

    @Test
    void testRepositoryWithNullValues() {
        // Given - Create entity with null values
        WithholdingTaxEntity nullEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_NULL_001")
                .underlying("TEST")
                .currency("USD")
                .grossDividendAmount(1000.0)
                .netDividendAmount(1000.0)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        entityManager.persistAndFlush(nullEntity);

        // When
        List<WithholdingTaxEntity> result = withholdingTaxRepository.findByContractId("EQ_SWAP_NULL_001");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EQ_SWAP_NULL_001", result.get(0).getContractId());
        assertNull(result.get(0).getWithholdingTaxAmount());
        assertNull(result.get(0).getTaxJurisdiction());
    }

    @Test
    void testRepositoryPagination() {
        // Given - Create multiple entities for the same contract
        for (int i = 1; i <= 5; i++) {
            WithholdingTaxEntity entity = WithholdingTaxEntity.builder()
                    .contractId("EQ_SWAP_PAGINATION_001")
                    .underlying("IBM")
                    .currency("USD")
                    .grossDividendAmount(1000.0 * i)
                    .netDividendAmount(850.0 * i)
                    .calculationDate(LocalDate.of(2024, 1, 15))
                    .calculationType("LOT_BASED")
                    .build();
            entityManager.persistAndFlush(entity);
        }

        // When
        List<WithholdingTaxEntity> result = withholdingTaxRepository.findByContractId("EQ_SWAP_PAGINATION_001");

        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // Verify all entities belong to the same contract
        for (WithholdingTaxEntity entity : result) {
            assertEquals("EQ_SWAP_PAGINATION_001", entity.getContractId());
        }
    }
}
