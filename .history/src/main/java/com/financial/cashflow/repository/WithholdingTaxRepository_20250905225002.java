package com.financial.cashflow.repository;

import com.financial.cashflow.entity.WithholdingTaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for WithholdingTaxEntity
 */
@Repository
public interface WithholdingTaxRepository extends JpaRepository<WithholdingTaxEntity, Long> {
    
    /**
     * Find withholding tax details by contract ID
     */
    List<WithholdingTaxEntity> findByContractId(String contractId);
    
    /**
     * Find withholding tax details by contract ID and date range
     */
    @Query("SELECT w FROM WithholdingTaxEntity w WHERE w.contractId = :contractId " +
           "AND w.calculationDate BETWEEN :fromDate AND :toDate")
    List<WithholdingTaxEntity> findByContractIdAndDateRange(@Param("contractId") String contractId,
                                                           @Param("fromDate") LocalDate fromDate,
                                                           @Param("toDate") LocalDate toDate);
    
    /**
     * Find withholding tax details by tax utility reference
     */
    List<WithholdingTaxEntity> findByTaxUtilityReference(String taxUtilityReference);
    
    /**
     * Find withholding tax details by tax jurisdiction
     */
    List<WithholdingTaxEntity> findByTaxJurisdiction(String taxJurisdiction);
    
    /**
     * Find withholding tax details by calculation type
     */
    List<WithholdingTaxEntity> findByCalculationType(String calculationType);
    
    /**
     * Find withholding tax details for tax utility reporting
     */
    @Query("SELECT w FROM WithholdingTaxEntity w WHERE w.taxJurisdiction = :jurisdiction " +
           "AND w.calculationDate BETWEEN :fromDate AND :toDate " +
           "AND w.withholdingTaxAmount > 0")
    List<WithholdingTaxEntity> findForTaxUtilityReporting(@Param("jurisdiction") String jurisdiction,
                                                         @Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate);
    
    /**
     * Get total withholding tax amount by contract
     */
    @Query("SELECT SUM(w.withholdingTaxAmount) FROM WithholdingTaxEntity w WHERE w.contractId = :contractId")
    Double getTotalWithholdingTaxByContract(@Param("contractId") String contractId);
    
    /**
     * Get total withholding tax amount by jurisdiction
     */
    @Query("SELECT SUM(w.withholdingTaxAmount) FROM WithholdingTaxEntity w WHERE w.taxJurisdiction = :jurisdiction " +
           "AND w.calculationDate BETWEEN :fromDate AND :toDate")
    Double getTotalWithholdingTaxByJurisdiction(@Param("jurisdiction") String jurisdiction,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);
}
