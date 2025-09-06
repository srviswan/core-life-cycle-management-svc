package com.financial.cashflow.repository;

import com.financial.cashflow.model.SettlementInstruction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for settlement instruction operations
 */
@Repository
public interface SettlementRepository extends JpaRepository<SettlementInstruction, String> {
    
    /**
     * Find settlement by natural key components
     */
    @Query("SELECT s FROM SettlementInstruction s WHERE s.contractId = :contractId " +
           "AND s.cashFlowId = :cashFlowId AND s.settlementDate = :settlementDate " +
           "AND s.settlementType = :settlementType")
    SettlementInstruction findByNaturalKey(
            @Param("contractId") String contractId,
            @Param("cashFlowId") String cashFlowId,
            @Param("settlementDate") LocalDate settlementDate,
            @Param("settlementType") String settlementType);
    
    /**
     * Find settlements with filters and pagination
     */
    @Query("SELECT s FROM SettlementInstruction s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:counterparty IS NULL OR s.counterparty = :counterparty) AND " +
           "(:currency IS NULL OR s.currency = :currency) AND " +
           "(:settlementType IS NULL OR s.settlementType = :settlementType) AND " +
           "(:fromDate IS NULL OR s.settlementDate >= :fromDate) AND " +
           "(:toDate IS NULL OR s.settlementDate <= :toDate) " +
           "ORDER BY s.createdAt DESC")
    List<SettlementInstruction> findSettlementsWithFilters(
            @Param("status") String status,
            @Param("counterparty") String counterparty,
            @Param("currency") String currency,
            @Param("settlementType") String settlementType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("offset") int offset,
            @Param("size") int size);
    
    /**
     * Count settlements with filters
     */
    @Query("SELECT COUNT(s) FROM SettlementInstruction s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:counterparty IS NULL OR s.counterparty = :counterparty) AND " +
           "(:currency IS NULL OR s.currency = :currency) AND " +
           "(:settlementType IS NULL OR s.settlementType = :settlementType) AND " +
           "(:fromDate IS NULL OR s.settlementDate >= :fromDate) AND " +
           "(:toDate IS NULL OR s.settlementDate <= :toDate)")
    Long countSettlementsWithFilters(
            @Param("status") String status,
            @Param("counterparty") String counterparty,
            @Param("currency") String currency,
            @Param("settlementType") String settlementType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
    
    /**
     * Count settlements by status
     */
    @Query("SELECT COUNT(s) FROM SettlementInstruction s WHERE s.status = :status")
    Integer countByStatus(@Param("status") String status);
    
    /**
     * Count settlements by type
     */
    @Query("SELECT COUNT(s) FROM SettlementInstruction s WHERE s.settlementType = :settlementType")
    Integer countByType(@Param("settlementType") String settlementType);
    
    /**
     * Get total amount by filters
     */
    @Query("SELECT SUM(s.amount) FROM SettlementInstruction s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:counterparty IS NULL OR s.counterparty = :counterparty) AND " +
           "(:currency IS NULL OR s.currency = :currency) AND " +
           "(:settlementType IS NULL OR s.settlementType = :settlementType) AND " +
           "(:fromDate IS NULL OR s.settlementDate >= :fromDate) AND " +
           "(:toDate IS NULL OR s.settlementDate <= :toDate)")
    Double getTotalAmountByFilters(
            @Param("status") String status,
            @Param("counterparty") String counterparty,
            @Param("currency") String currency,
            @Param("settlementType") String settlementType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
    
    /**
     * Find settlements by contract ID
     */
    List<SettlementInstruction> findByContractId(String contractId);
    
    /**
     * Find settlements by counterparty
     */
    List<SettlementInstruction> findByCounterparty(String counterparty);
    
    /**
     * Find settlements by status
     */
    List<SettlementInstruction> findByStatus(String status);
}
