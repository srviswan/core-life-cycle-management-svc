package com.financial.cashflow.service;

import com.financial.cashflow.dto.*;
import com.financial.cashflow.model.SettlementInstruction;
import com.financial.cashflow.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing settlement instructions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {
    
    private final SettlementRepository settlementRepository;
    
    /**
     * Get all settlements with filtering and pagination
     */
    public SettlementResponse getAllSettlements(
            String status, String counterparty, String currency, String settlementType,
            LocalDate fromDate, LocalDate toDate, Integer page, Integer size) {
        
        log.info("Getting settlements with filters: status={}, counterparty={}, currency={}", 
                status, counterparty, currency);
        
        // Calculate pagination
        int offset = (page - 1) * size;
        
        List<SettlementInstruction> settlements = settlementRepository.findSettlementsWithFilters(
                status, counterparty, currency, settlementType, fromDate, toDate, offset, size);
        
        Long totalCount = settlementRepository.countSettlementsWithFilters(
                status, counterparty, currency, settlementType, fromDate, toDate);
        
        // Build pagination info
        int totalPages = (int) Math.ceil((double) totalCount / size);
        SettlementResponse.PaginationInfo pagination = SettlementResponse.PaginationInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalCount)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();
        
        // Build summary
        SettlementResponse.SettlementSummary summary = buildSettlementSummary(
                status, counterparty, currency, settlementType, fromDate, toDate);
        
        return SettlementResponse.builder()
                .settlements(settlements)
                .pagination(pagination)
                .summary(summary)
                .build();
    }
    
    /**
     * Create a new settlement instruction
     */
    @Transactional
    public SettlementInstruction createSettlement(SettlementCreateRequest request) {
        log.info("Creating settlement for contract: {}, cashFlow: {}", 
                request.getContractId(), request.getCashFlowId());
        
        // Check if settlement with same natural key already exists
        SettlementInstruction existing = settlementRepository.findByNaturalKey(
                request.getContractId(), request.getCashFlowId(), 
                request.getSettlementDate(), request.getSettlementType());
        
        if (existing != null) {
            throw new IllegalArgumentException("Settlement with same natural key already exists");
        }
        
        SettlementInstruction settlement = SettlementInstruction.builder()
                .settlementId(UUID.randomUUID().toString())
                .contractId(request.getContractId())
                .cashFlowId(request.getCashFlowId())
                .settlementDate(request.getSettlementDate())
                .settlementType(request.getSettlementType())
                .counterparty(request.getCounterparty())
                .amount(java.math.BigDecimal.valueOf(request.getAmount()))
                .currency(request.getCurrency())
                .notes(request.getNotes())
                .status(SettlementInstruction.Status.PENDING.getValue())
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return settlementRepository.save(settlement);
    }
    
    /**
     * Get pending settlements
     */
    public SettlementResponse getPendingSettlements(
            String counterparty, String currency, String settlementType, 
            Integer page, Integer size) {
        
        return getAllSettlements(
                SettlementInstruction.Status.PENDING.getValue(),
                counterparty, currency, settlementType, null, null, page, size);
    }
    
    /**
     * Get settlement by ID
     */
    public SettlementInstruction getSettlementById(String settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));
    }
    
    /**
     * Get settlement by natural key
     */
    public SettlementInstruction getSettlementByNaturalKey(
            String contractId, String cashFlowId, LocalDate settlementDate, String settlementType) {
        
        SettlementInstruction settlement = settlementRepository.findByNaturalKey(
                contractId, cashFlowId, settlementDate, settlementType);
        
        if (settlement == null) {
            throw new IllegalArgumentException("Settlement not found for natural key");
        }
        
        return settlement;
    }
    
    /**
     * Update settlement status
     */
    @Transactional
    public SettlementInstruction updateSettlementStatus(String settlementId, SettlementStatusUpdate update) {
        log.info("Updating settlement status: {} to {}", settlementId, update.getStatus());
        
        SettlementInstruction settlement = getSettlementById(settlementId);
        
        // Validate status transition
        validateStatusTransition(settlement.getStatus(), update.getStatus());
        
        // Update fields
        settlement.setStatus(update.getStatus());
        settlement.setActualSettlementDate(update.getActualSettlementDate());
        settlement.setSettlementReference(update.getSettlementReference());
        settlement.setNotes(update.getNotes());
        // Note: retryReason is not a field in SettlementInstruction model
        settlement.setNextRetryDate(update.getNextRetryDate());
        settlement.setCancellationReason(update.getCancellationReason());
        settlement.setCancelledBy(update.getCancelledBy());
        settlement.setUpdatedAt(LocalDateTime.now());
        
        // Update retry count if retrying
        if ("PENDING".equals(update.getStatus()) && "FAILED".equals(settlement.getStatus())) {
            settlement.setRetryCount(settlement.getRetryCount() + 1);
            settlement.setLastRetryDate(LocalDateTime.now());
        }
        
        return settlementRepository.save(settlement);
    }
    
    /**
     * Bulk update settlement status
     */
    @Transactional
    public BulkSettlementStatusResponse bulkUpdateSettlementStatus(BulkSettlementStatusUpdate update) {
        log.info("Bulk updating {} settlements to status: {}", 
                update.getSettlementIds().size(), update.getStatus());
        
        List<BulkSettlementStatusResponse.BulkSettlementResult> results = 
                update.getSettlementIds().stream()
                        .map(settlementId -> {
                            try {
                                SettlementStatusUpdate statusUpdate = SettlementStatusUpdate.builder()
                                        .status(update.getStatus())
                                        .actualSettlementDate(update.getActualSettlementDate())
                                        .settlementReference(update.getSettlementReference())
                                        .notes(update.getNotes())
                                        .build();
                                
                                updateSettlementStatus(settlementId, statusUpdate);
                                
                                return BulkSettlementStatusResponse.BulkSettlementResult.builder()
                                        .settlementId(settlementId)
                                        .success(true)
                                        .updatedStatus(update.getStatus())
                                        .build();
                            } catch (Exception e) {
                                log.error("Failed to update settlement: {}", settlementId, e);
                                return BulkSettlementStatusResponse.BulkSettlementResult.builder()
                                        .settlementId(settlementId)
                                        .success(false)
                                        .errorMessage(e.getMessage())
                                        .build();
                            }
                        })
                        .collect(Collectors.toList());
        
        long successCount = results.stream().mapToLong(r -> r.getSuccess() ? 1 : 0).sum();
        
        return BulkSettlementStatusResponse.builder()
                .totalRequested(update.getSettlementIds().size())
                .totalUpdated((int) successCount)
                .results(results)
                .build();
    }
    
    /**
     * Get settlement summary
     */
    public SettlementResponse.SettlementSummary getSettlementSummary(
            LocalDate fromDate, LocalDate toDate, String counterparty, String currency) {
        
        return buildSettlementSummary(null, counterparty, currency, null, fromDate, toDate);
    }
    
    /**
     * Build settlement summary
     */
    private SettlementResponse.SettlementSummary buildSettlementSummary(
            String status, String counterparty, String currency, String settlementType,
            LocalDate fromDate, LocalDate toDate) {
        
        // Get counts by status
        SettlementResponse.SettlementSummary.StatusBreakdown statusBreakdown = 
                SettlementResponse.SettlementSummary.StatusBreakdown.builder()
                        .pending(settlementRepository.countByStatus(SettlementInstruction.Status.PENDING.getValue()))
                        .processing(settlementRepository.countByStatus(SettlementInstruction.Status.PROCESSING.getValue()))
                        .completed(settlementRepository.countByStatus(SettlementInstruction.Status.COMPLETED.getValue()))
                        .failed(settlementRepository.countByStatus(SettlementInstruction.Status.FAILED.getValue()))
                        .cancelled(settlementRepository.countByStatus(SettlementInstruction.Status.CANCELLED.getValue()))
                        .build();
        
        // Get counts by type
        SettlementResponse.SettlementSummary.TypeBreakdown typeBreakdown = 
                SettlementResponse.SettlementSummary.TypeBreakdown.builder()
                        .interest(settlementRepository.countByType(SettlementInstruction.SettlementType.INTEREST.getValue()))
                        .equity(settlementRepository.countByType(SettlementInstruction.SettlementType.EQUITY.getValue()))
                        .dividend(settlementRepository.countByType(SettlementInstruction.SettlementType.DIVIDEND.getValue()))
                        .principal(settlementRepository.countByType(SettlementInstruction.SettlementType.PRINCIPAL.getValue()))
                        .build();
        
        // Get total amount
        Double totalAmount = settlementRepository.getTotalAmountByFilters(
                status, counterparty, currency, settlementType, fromDate, toDate);
        
        Integer totalSettlements = statusBreakdown.getPending() + statusBreakdown.getProcessing() + 
                statusBreakdown.getCompleted() + statusBreakdown.getFailed() + statusBreakdown.getCancelled();
        
        return SettlementResponse.SettlementSummary.builder()
                .totalSettlements(totalSettlements)
                .totalAmount(totalAmount != null ? totalAmount : 0.0)
                .currency(currency != null ? currency : "USD")
                .statusBreakdown(statusBreakdown)
                .typeBreakdown(typeBreakdown)
                .build();
    }
    
    /**
     * Validate status transition
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Define valid transitions
        switch (currentStatus) {
            case "PENDING":
                if (!List.of("PROCESSING", "CANCELLED").contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from PENDING to " + newStatus);
                }
                break;
            case "PROCESSING":
                if (!List.of("COMPLETED", "FAILED", "CANCELLED").contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from PROCESSING to " + newStatus);
                }
                break;
            case "FAILED":
                if (!List.of("PENDING", "CANCELLED").contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from FAILED to " + newStatus);
                }
                break;
            case "COMPLETED":
            case "CANCELLED":
                throw new IllegalArgumentException("Cannot change status from " + currentStatus);
            default:
                throw new IllegalArgumentException("Unknown current status: " + currentStatus);
        }
    }
}
