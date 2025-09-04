package com.financial.cashflow.repository;

import com.financial.cashflow.entity.CashFlow;
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
 * Repository for CashFlow entity operations.
 */
@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {

    /**
     * Find cash flows by contract ID.
     */
    List<CashFlow> findByContractId(String contractId);

    /**
     * Find cash flows by contract ID with pagination.
     */
    Page<CashFlow> findByContractId(String contractId, Pageable pageable);

    /**
     * Find cash flows by contract ID and date range.
     */
    @Query("SELECT cf FROM CashFlow cf WHERE cf.contractId = :contractId " +
           "AND cf.cashFlowDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY cf.cashFlowDate")
    List<CashFlow> findByContractIdAndDateRange(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find cash flows by contract ID, date range, and type.
     */
    @Query("SELECT cf FROM CashFlow cf WHERE cf.contractId = :contractId " +
           "AND cf.cashFlowDate BETWEEN :fromDate AND :toDate " +
           "AND cf.cashFlowType = :cashFlowType " +
           "ORDER BY cf.cashFlowDate")
    List<CashFlow> findByContractIdAndDateRangeAndType(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("cashFlowType") String cashFlowType);

    /**
     * Find cash flows by status.
     */
    List<CashFlow> findByStatus(String status);

    /**
     * Find cash flows by status with pagination.
     */
    Page<CashFlow> findByStatus(String status, Pageable pageable);

    /**
     * Find cash flows by lot ID.
     */
    List<CashFlow> findByLotId(String lotId);

    /**
     * Find cash flows by request ID.
     */
    List<CashFlow> findByRequestId(String requestId);

    /**
     * Find cash flows by calculation ID.
     */
    List<CashFlow> findByCalculationId(String calculationId);

    /**
     * Find cash flow by cash flow ID.
     */
    Optional<CashFlow> findByCashFlowId(String cashFlowId);

    /**
     * Find cash flows by date range.
     */
    @Query("SELECT cf FROM CashFlow cf WHERE cf.cashFlowDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY cf.cashFlowDate, cf.contractId")
    List<CashFlow> findByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find cash flows by date range with pagination.
     */
    @Query("SELECT cf FROM CashFlow cf WHERE cf.cashFlowDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY cf.cashFlowDate, cf.contractId")
    Page<CashFlow> findByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable);

    /**
     * Count cash flows by contract ID and status.
     */
    long countByContractIdAndStatus(String contractId, String status);

    /**
     * Sum amounts by contract ID and cash flow type.
     */
    @Query("SELECT SUM(cf.amount) FROM CashFlow cf WHERE cf.contractId = :contractId " +
           "AND cf.cashFlowType = :cashFlowType")
    java.math.BigDecimal sumAmountByContractIdAndType(
        @Param("contractId") String contractId,
        @Param("cashFlowType") String cashFlowType);

    /**
     * Sum amounts by contract ID and date range.
     */
    @Query("SELECT SUM(cf.amount) FROM CashFlow cf WHERE cf.contractId = :contractId " +
           "AND cf.cashFlowDate BETWEEN :fromDate AND :toDate")
    java.math.BigDecimal sumAmountByContractIdAndDateRange(
        @Param("contractId") String contractId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    /**
     * Find cash flows for settlement (REALIZED_UNSETTLED status).
     */
    @Query("SELECT cf FROM CashFlow cf WHERE cf.status = 'REALIZED_UNSETTLED' " +
           "AND cf.settlementDate <= :settlementDate " +
           "ORDER BY cf.settlementDate, cf.cashFlowId")
    List<CashFlow> findPendingSettlements(@Param("settlementDate") LocalDate settlementDate);

    /**
     * Delete cash flows by request ID.
     */
    void deleteByRequestId(String requestId);

    /**
     * Delete cash flows by calculation ID.
     */
    void deleteByCalculationId(String calculationId);

    /**
     * Check if cash flow exists by cash flow ID.
     */
    boolean existsByCashFlowId(String cashFlowId);
}
