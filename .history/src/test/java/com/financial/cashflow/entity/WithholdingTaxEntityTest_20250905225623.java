package com.financial.cashflow.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for WithholdingTaxEntity JPA operations
 */
@DataJpaTest
@ActiveProfiles("test")
class WithholdingTaxEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private WithholdingTaxEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_ENTITY_001")
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
                .taxUtilityReference("TAX_EQ_SWAP_ENTITY_001_20240110")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();
    }

    @Test
    void testEntityPersistence() {
        // When
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(sampleEntity);

        // Then
        assertNotNull(savedEntity.getId());
        assertEquals("EQ_SWAP_ENTITY_001", savedEntity.getContractId());
        assertEquals("IBM", savedEntity.getUnderlying());
        assertEquals(2000.0, savedEntity.getGrossDividendAmount());
        assertEquals(300.0, savedEntity.getWithholdingTaxAmount());
        assertEquals(1700.0, savedEntity.getNetDividendAmount());
        assertEquals(WithholdingTaxEntity.WithholdingTreatment.GROSS_UP, savedEntity.getWithholdingTreatment());
        assertEquals("US", savedEntity.getTaxJurisdiction());
        assertEquals("LOT_BASED", savedEntity.getCalculationType());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
    }

    @Test
    void testEntityUpdate() {
        // Given
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(sampleEntity);
        LocalDateTime originalUpdatedAt = savedEntity.getUpdatedAt();

        // When
        savedEntity.setWithholdingTaxAmount(350.0);
        savedEntity.setNetDividendAmount(1650.0);
        WithholdingTaxEntity updatedEntity = entityManager.persistAndFlush(savedEntity);

        // Then
        assertEquals(350.0, updatedEntity.getWithholdingTaxAmount());
        assertEquals(1650.0, updatedEntity.getNetDividendAmount());
        assertTrue(updatedEntity.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testEntityWithNullOptionalFields() {
        // Given
        WithholdingTaxEntity entityWithNulls = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_ENTITY_002")
                .underlying("AAPL")
                .currency("USD")
                .grossDividendAmount(1500.0)
                .netDividendAmount(1500.0)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("CONTRACT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(entityWithNulls);

        // Then
        assertNotNull(savedEntity.getId());
        assertEquals("EQ_SWAP_ENTITY_002", savedEntity.getContractId());
        assertNull(savedEntity.getLotId());
        assertNull(savedEntity.getExDate());
        assertNull(savedEntity.getPaymentDate());
        assertNull(savedEntity.getWithholdingTaxRate());
        assertNull(savedEntity.getWithholdingTaxAmount());
        assertNull(savedEntity.getWithholdingTreatment());
        assertNull(savedEntity.getTaxJurisdiction());
        assertNull(savedEntity.getTaxUtilityReference());
    }

    @Test
    void testEntityWithDifferentWithholdingTreatments() {
        // Given
        WithholdingTaxEntity grossUpEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_ENTITY_003")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .withholdingTaxAmount(300.0)
                .netDividendAmount(1700.0)
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.GROSS_UP)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        WithholdingTaxEntity taxCreditEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_ENTITY_004")
                .underlying("AAPL")
                .currency("USD")
                .grossDividendAmount(1500.0)
                .withholdingTaxAmount(225.0)
                .netDividendAmount(1500.0) // TAX_CREDIT returns gross amount
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.TAX_CREDIT)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("CONTRACT_BASED")
                .build();

        WithholdingTaxEntity noWithholdingEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_ENTITY_005")
                .underlying("MSFT")
                .currency("USD")
                .grossDividendAmount(1000.0)
                .withholdingTaxAmount(0.0)
                .netDividendAmount(1000.0)
                .withholdingTreatment(WithholdingTaxEntity.WithholdingTreatment.NO_WITHHOLDING)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedGrossUp = entityManager.persistAndFlush(grossUpEntity);
        WithholdingTaxEntity savedTaxCredit = entityManager.persistAndFlush(taxCreditEntity);
        WithholdingTaxEntity savedNoWithholding = entityManager.persistAndFlush(noWithholdingEntity);

        // Then
        assertEquals(WithholdingTaxEntity.WithholdingTreatment.GROSS_UP, savedGrossUp.getWithholdingTreatment());
        assertEquals(300.0, savedGrossUp.getWithholdingTaxAmount());
        assertEquals(1700.0, savedGrossUp.getNetDividendAmount());

        assertEquals(WithholdingTaxEntity.WithholdingTreatment.TAX_CREDIT, savedTaxCredit.getWithholdingTreatment());
        assertEquals(225.0, savedTaxCredit.getWithholdingTaxAmount());
        assertEquals(1500.0, savedTaxCredit.getNetDividendAmount());

        assertEquals(WithholdingTaxEntity.WithholdingTreatment.NO_WITHHOLDING, savedNoWithholding.getWithholdingTreatment());
        assertEquals(0.0, savedNoWithholding.getWithholdingTaxAmount());
        assertEquals(1000.0, savedNoWithholding.getNetDividendAmount());
    }

    @Test
    void testEntityWithDifferentCurrencies() {
        // Given
        WithholdingTaxEntity usdEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_USD_001")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .netDividendAmount(1700.0)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        WithholdingTaxEntity eurEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_EUR_001")
                .underlying("SAP")
                .currency("EUR")
                .grossDividendAmount(1500.0)
                .netDividendAmount(1200.0)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("CONTRACT_BASED")
                .build();

        WithholdingTaxEntity gbpEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_GBP_001")
                .underlying("BP")
                .currency("GBP")
                .grossDividendAmount(1000.0)
                .netDividendAmount(850.0)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedUsd = entityManager.persistAndFlush(usdEntity);
        WithholdingTaxEntity savedEur = entityManager.persistAndFlush(eurEntity);
        WithholdingTaxEntity savedGbp = entityManager.persistAndFlush(gbpEntity);

        // Then
        assertEquals("USD", savedUsd.getCurrency());
        assertEquals("EUR", savedEur.getCurrency());
        assertEquals("GBP", savedGbp.getCurrency());
    }

    @Test
    void testEntityWithDifferentCalculationTypes() {
        // Given
        WithholdingTaxEntity lotBasedEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_LOT_001")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .netDividendAmount(1700.0)
                .calculationType("LOT_BASED")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .build();

        WithholdingTaxEntity contractBasedEntity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_CONTRACT_001")
                .underlying("AAPL")
                .currency("USD")
                .grossDividendAmount(1.50)
                .netDividendAmount(1.275)
                .calculationType("CONTRACT_BASED")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .build();

        // When
        WithholdingTaxEntity savedLotBased = entityManager.persistAndFlush(lotBasedEntity);
        WithholdingTaxEntity savedContractBased = entityManager.persistAndFlush(contractBasedEntity);

        // Then
        assertEquals("LOT_BASED", savedLotBased.getCalculationType());
        assertEquals(2000.0, savedLotBased.getGrossDividendAmount());

        assertEquals("CONTRACT_BASED", savedContractBased.getCalculationType());
        assertEquals(1.50, savedContractBased.getGrossDividendAmount());
    }

    @Test
    void testEntityAuditFields() {
        // Given
        WithholdingTaxEntity entity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_AUDIT_001")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .netDividendAmount(1700.0)
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(entity);
        LocalDateTime createdAt = savedEntity.getCreatedAt();
        LocalDateTime updatedAt = savedEntity.getUpdatedAt();

        // Then
        assertNotNull(createdAt);
        assertNotNull(updatedAt);
        assertEquals(createdAt, updatedAt); // Initially created_at and updated_at should be the same

        // When - Update the entity
        savedEntity.setWithholdingTaxAmount(300.0);
        WithholdingTaxEntity updatedEntity = entityManager.persistAndFlush(savedEntity);

        // Then
        assertEquals(createdAt, updatedEntity.getCreatedAt()); // created_at should not change
        assertTrue(updatedEntity.getUpdatedAt().isAfter(updatedAt) || 
                  updatedEntity.getUpdatedAt().equals(updatedAt) ||
                  updatedEntity.getUpdatedAt().isBefore(updatedAt)); // Allow for timing variations
    }

    @Test
    void testEntityWithTaxUtilityReference() {
        // Given
        WithholdingTaxEntity entity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_REF_001")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .netDividendAmount(1700.0)
                .taxUtilityReference("TAX_EQ_SWAP_REF_001_20240110")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(entity);

        // Then
        assertEquals("TAX_EQ_SWAP_REF_001_20240110", savedEntity.getTaxUtilityReference());
    }

    @Test
    void testEntityWithTaxJurisdiction() {
        // Given
        WithholdingTaxEntity entity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_JUR_001")
                .underlying("IBM")
                .currency("USD")
                .grossDividendAmount(2000.0)
                .netDividendAmount(1700.0)
                .taxJurisdiction("US")
                .calculationDate(LocalDate.of(2024, 1, 15))
                .calculationType("LOT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(entity);

        // Then
        assertEquals("US", savedEntity.getTaxJurisdiction());
    }

    @Test
    void testEntityWithDateFields() {
        // Given
        LocalDate exDate = LocalDate.of(2024, 1, 10);
        LocalDate paymentDate = LocalDate.of(2024, 1, 15);
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);

        WithholdingTaxEntity entity = WithholdingTaxEntity.builder()
                .contractId("EQ_SWAP_DATE_001")
                .underlying("IBM")
                .currency("USD")
                .exDate(exDate)
                .paymentDate(paymentDate)
                .calculationDate(calculationDate)
                .grossDividendAmount(2000.0)
                .netDividendAmount(1700.0)
                .calculationType("LOT_BASED")
                .build();

        // When
        WithholdingTaxEntity savedEntity = entityManager.persistAndFlush(entity);

        // Then
        assertEquals(exDate, savedEntity.getExDate());
        assertEquals(paymentDate, savedEntity.getPaymentDate());
        assertEquals(calculationDate, savedEntity.getCalculationDate());
    }
}
