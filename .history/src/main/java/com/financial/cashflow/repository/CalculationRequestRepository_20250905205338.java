package com.financial.cashflow.repository;

import com.financial.cashflow.dto.CalculationRequestsResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for calculation request audit trail
 */
@Repository
public interface CalculationRequestRepository extends JpaRepository<com.financial.cashflow.repository.CalculationRequestEntity, String> {
    
    /**
     * Find calculation request by request ID
     */
    CalculationRequestsResponse.CalculationRequestInfo findByRequestId(String requestId);
    
    /**
     * Find calculation requests by contract with filters
     */
    @Query(value = "SELECT new com.financial.cashflow.dto.CalculationRequestsResponse$CalculationRequestInfo(" +
           "cr.requestId, cr.contractId, cr.calculationType, cr.fromDate, cr.toDate, " +
           "cr.status, cr.progressPercentage, cr.errorMessage, cr.inputDataHash, " +
           "cr.createdAt, cr.updatedAt) " +
           "FROM CalculationRequestEntity cr WHERE cr.contractId = :contractId " +
           "AND (:fromDate IS NULL OR cr.fromDate >= :fromDate) " +
           "AND (:toDate IS NULL OR cr.toDate <= :toDate) " +
           "AND (:calculationType IS NULL OR cr.calculationType = :calculationType) " +
           "AND (:status IS NULL OR cr.status = :status) " +
           "ORDER BY cr.createdAt DESC")
    List<CalculationRequestsResponse.CalculationRequestInfo> findByContractWithFilters(
            @Param("contractId") String contractId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("calculationType") String calculationType,
            @Param("status") String status,
            Pageable pageable);
    
    /**
     * Count calculation requests by contract with filters
     */
    @Query("SELECT COUNT(cr) FROM CalculationRequestEntity cr WHERE cr.contractId = :contractId " +
           "AND (:fromDate IS NULL OR cr.fromDate >= :fromDate) " +
           "AND (:toDate IS NULL OR cr.toDate <= :toDate) " +
           "AND (:calculationType IS NULL OR cr.calculationType = :calculationType) " +
           "AND (:status IS NULL OR cr.status = :status)")
    Long countByContractWithFilters(
            @Param("contractId") String contractId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("calculationType") String calculationType,
            @Param("status") String status);
    
    /**
     * Count calculation requests by status
     */
    @Query("SELECT COUNT(cr) FROM CalculationRequestEntity cr WHERE cr.status = :status")
    Integer countByStatus(@Param("status") String status);
}
