package com.financial.cashflow.repository;

import com.financial.cashflow.entity.CalculationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CalculationRequest entity operations.
 */
@Repository
public interface CalculationRequestRepository extends JpaRepository<CalculationRequest, Long> {

    /**
     * Find calculation request by request ID.
     */
    Optional<CalculationRequest> findByRequestId(String requestId);

    /**
     * Find calculation requests by contract ID.
     */
    List<CalculationRequest> findByContractId(String contractId);

    /**
     * Find calculation requests by contract ID with pagination.
     */
    Page<CalculationRequest> findByContractId(String contractId, Pageable pageable);

    /**
     * Find calculation requests by calculation ID.
     */
    List<CalculationRequest> findByCalculationId(String calculationId);

    /**
     * Find calculation requests by status.
     */
    List<CalculationRequest> findByStatus(String status);

    /**
     * Find calculation requests by status with pagination.
     */
    Page<CalculationRequest> findByStatus(String status, Pageable pageable);

    /**
     * Find calculation requests by calculation type.
     */
    List<CalculationRequest> findByCalculationType(String calculationType);

    /**
     * Find calculation requests by date range.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.fromDate BETWEEN :fromDate AND :toDate " +
           "OR cr.toDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY cr.createdAt DESC")
    List<CalculationRequest> findByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find calculation requests by date range with pagination.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.fromDate BETWEEN :fromDate AND :toDate " +
           "OR cr.toDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY cr.createdAt DESC")
    Page<CalculationRequest> findByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable);

    /**
     * Find calculation requests by contract ID and date range.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.contractId = :contractId " +
           "AND cr.fromDate BETWEEN :fromDate AND :toDate " +
           "OR cr.toDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY cr.createdAt DESC")
    List<CalculationRequest> findByContractIdAndDateRange(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find calculation requests by natural key.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.contractId = :contractId " +
           "AND cr.fromDate = :fromDate " +
           "AND cr.toDate = :toDate " +
           "AND cr.calculationType = :calculationType")
    Optional<CalculationRequest> findByNaturalKey(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("calculationType") String calculationType);

    /**
     * Find calculation requests by input data hash.
     */
    List<CalculationRequest> findByInputDataHash(String inputDataHash);

    /**
     * Find calculation requests created after a specific date.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.createdAt >= :createdAfter " +
           "ORDER BY cr.createdAt DESC")
    List<CalculationRequest> findByCreatedAfter(@Param("createdAfter") java.time.LocalDateTime createdAfter);

    /**
     * Find calculation requests created after a specific date with pagination.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.createdAt >= :createdAfter " +
           "ORDER BY cr.createdAt DESC")
    Page<CalculationRequest> findByCreatedAfter(
        @Param("createdAfter") java.time.LocalDateTime createdAfter,
        Pageable pageable);

    /**
     * Find calculation requests by contract ID and calculation type.
     */
    List<CalculationRequest> findByContractIdAndCalculationType(String contractId, String calculationType);

    /**
     * Find calculation requests by contract ID and status.
     */
    List<CalculationRequest> findByContractIdAndStatus(String contractId, String status);

    /**
     * Count calculation requests by status.
     */
    long countByStatus(String status);

    /**
     * Count calculation requests by contract ID and status.
     */
    long countByContractIdAndStatus(String contractId, String status);

    /**
     * Count calculation requests by calculation type.
     */
    long countByCalculationType(String calculationType);

    /**
     * Find calculation requests with errors.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.status = 'FAILED' " +
           "ORDER BY cr.createdAt DESC")
    List<CalculationRequest> findFailedRequests();

    /**
     * Find calculation requests with errors with pagination.
     */
    @Query("SELECT cr FROM CalculationRequest cr WHERE cr.status = 'FAILED' " +
           "ORDER BY cr.createdAt DESC")
    Page<CalculationRequest> findFailedRequests(Pageable pageable);

    /**
     * Find calculation requests by contract ID and calculation type with pagination.
     */
    Page<CalculationRequest> findByContractIdAndCalculationType(
        String contractId, String calculationType, Pageable pageable);

    /**
     * Check if calculation request exists by request ID.
     */
    boolean existsByRequestId(String requestId);

    /**
     * Check if calculation request exists by natural key.
     */
    @Query("SELECT COUNT(cr) > 0 FROM CalculationRequest cr WHERE cr.contractId = :contractId " +
           "AND cr.fromDate = :fromDate " +
           "AND cr.toDate = :toDate " +
           "AND cr.calculationType = :calculationType")
    boolean existsByNaturalKey(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("calculationType") String calculationType);

    /**
     * Delete calculation requests by contract ID.
     */
    void deleteByContractId(String contractId);

    /**
     * Delete calculation requests by calculation ID.
     */
    void deleteByCalculationId(String calculationId);
}
