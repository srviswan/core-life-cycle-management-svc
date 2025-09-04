package com.financial.cashflow.repository;

import com.financial.cashflow.entity.SettlementInstruction;
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
 * Repository for SettlementInstruction entity operations.
 */
@Repository
public interface SettlementInstructionRepository extends JpaRepository<SettlementInstruction, Long> {

    /**
     * Find settlement instructions by contract ID.
     */
    List<SettlementInstruction> findByContractId(String contractId);

    /**
     * Find settlement instructions by contract ID with pagination.
     */
    Page<SettlementInstruction> findByContractId(String contractId, Pageable pageable);

    /**
     * Find settlement instructions by cash flow ID.
     */
    List<SettlementInstruction> findByCashFlowId(String cashFlowId);

    /**
     * Find settlement instructions by settlement ID.
     */
    Optional<SettlementInstruction> findBySettlementId(String settlementId);

    /**
     * Find settlement instructions by status.
     */
    List<SettlementInstruction> findByStatus(String status);

    /**
     * Find settlement instructions by status with pagination.
     */
    Page<SettlementInstruction> findByStatus(String status, Pageable pageable);

    /**
     * Find settlement instructions by settlement date.
     */
    List<SettlementInstruction> findBySettlementDate(LocalDate settlementDate);

    /**
     * Find settlement instructions by settlement date range.
     */
    @Query("SELECT si FROM SettlementInstruction si WHERE si.settlementDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY si.settlementDate, si.priority")
    List<SettlementInstruction> findBySettlementDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find settlement instructions by settlement date range with pagination.
     */
    @Query("SELECT si FROM SettlementInstruction si WHERE si.settlementDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY si.settlementDate, si.priority")
    Page<SettlementInstruction> findBySettlementDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable);

    /**
     * Find settlement instructions by settlement type.
     */
    List<SettlementInstruction> findBySettlementType(String settlementType);

    /**
     * Find settlement instructions by priority.
     */
    List<SettlementInstruction> findByPriority(String priority);

    /**
     * Find settlement instructions by settlement method.
     */
    List<SettlementInstruction> findBySettlementMethod(String settlementMethod);

    /**
     * Find settlement instructions by request ID.
     */
    List<SettlementInstruction> findByRequestId(String requestId);

    /**
     * Find pending settlement instructions (PENDING status).
     */
    @Query("SELECT si FROM SettlementInstruction si WHERE si.status = 'PENDING' " +
           "AND si.settlementDate <= :settlementDate " +
           "ORDER BY si.priority, si.settlementDate")
    List<SettlementInstruction> findPendingSettlements(@Param("settlementDate") LocalDate settlementDate);

    /**
     * Find settlement instructions by contract ID and status.
     */
    List<SettlementInstruction> findByContractIdAndStatus(String contractId, String status);

    /**
     * Find settlement instructions by contract ID and settlement date range.
     */
    @Query("SELECT si FROM SettlementInstruction si WHERE si.contractId = :contractId " +
           "AND si.settlementDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY si.settlementDate")
    List<SettlementInstruction> findByContractIdAndSettlementDateRange(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find settlement instructions by natural key.
     */
    @Query("SELECT si FROM SettlementInstruction si WHERE si.contractId = :contractId " +
           "AND si.cashFlowId = :cashFlowId " +
           "AND si.settlementDate = :settlementDate " +
           "AND si.settlementType = :settlementType")
    Optional<SettlementInstruction> findByNaturalKey(
        @Param("contractId") String contractId,
        @Param("cashFlowId") String cashFlowId,
        @Param("settlementDate") LocalDate settlementDate,
        @Param("settlementType") String settlementType);

    /**
     * Count settlement instructions by status.
     */
    long countByStatus(String status);

    /**
     * Count settlement instructions by contract ID and status.
     */
    long countByContractIdAndStatus(String contractId, String status);

    /**
     * Sum amounts by contract ID and status.
     */
    @Query("SELECT SUM(si.amount) FROM SettlementInstruction si WHERE si.contractId = :contractId " +
           "AND si.status = :status")
    java.math.BigDecimal sumAmountByContractIdAndStatus(
        @Param("contractId") String contractId,
        @Param("status") String status);

    /**
     * Sum amounts by settlement date range.
     */
    @Query("SELECT SUM(si.amount) FROM SettlementInstruction si WHERE si.settlementDate BETWEEN :fromDate AND :toDate")
    java.math.BigDecimal sumAmountBySettlementDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Delete settlement instructions by request ID.
     */
    void deleteByRequestId(String requestId);

    /**
     * Check if settlement instruction exists by settlement ID.
     */
    boolean existsBySettlementId(String settlementId);

    /**
     * Check if settlement instruction exists by natural key.
     */
    @Query("SELECT COUNT(si) > 0 FROM SettlementInstruction si WHERE si.contractId = :contractId " +
           "AND si.cashFlowId = :cashFlowId " +
           "AND si.settlementDate = :settlementDate " +
           "AND si.settlementType = :settlementType")
    boolean existsByNaturalKey(
        @Param("contractId") String contractId,
        @Param("cashFlowId") String cashFlowId,
        @Param("settlementDate") LocalDate settlementDate,
        @Param("settlementType") String settlementType);
}
