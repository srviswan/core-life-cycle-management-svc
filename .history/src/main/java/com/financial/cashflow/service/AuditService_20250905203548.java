package com.financial.cashflow.service;

import com.financial.cashflow.dto.CalculationReproduction;
import com.financial.cashflow.dto.CalculationRequestsResponse;
import com.financial.cashflow.dto.SettlementResponse;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.repository.CalculationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for audit trail and calculation reproduction
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {
    
    private final CalculationRequestRepository calculationRequestRepository;
    private final CashFlowService cashFlowService;
    
    /**
     * Reproduce calculation from audit trail
     */
    public CalculationReproduction reproduceCalculation(String requestId) {
        log.info("Reproducing calculation for request: {}", requestId);
        
        // Get original request from audit trail
        CalculationRequestsResponse.CalculationRequestInfo originalRequest = 
                calculationRequestRepository.findByRequestId(requestId);
        
        if (originalRequest == null) {
            throw new IllegalArgumentException("Calculation request not found: " + requestId);
        }
        
        // Reconstruct the original request
        CashFlowRequest request = reconstructCashFlowRequest(originalRequest);
        
        // Reproduce the calculation
        CashFlowResponse reproducedResults = cashFlowService.calculate(request);
        
        // Perform integrity check
        CalculationReproduction.IntegrityCheck integrityCheck = performIntegrityCheck(
                originalRequest, reproducedResults);
        
        return CalculationReproduction.builder()
                .requestId(requestId)
                .originalRequest(request)
                .reproducedResults(reproducedResults)
                .integrityCheck(integrityCheck)
                .build();
    }
    
    /**
     * Get calculation requests by contract
     */
    public CalculationRequestsResponse getCalculationRequestsByContract(
            String contractId, LocalDate fromDate, LocalDate toDate, 
            String calculationType, String status, Integer page, Integer size) {
        
        log.info("Getting calculation requests for contract: {}", contractId);
        
        // Calculate pagination
        int offset = (page - 1) * size;
        
        List<CalculationRequestsResponse.CalculationRequestInfo> requests = 
                calculationRequestRepository.findByContractWithFilters(
                        contractId, fromDate, toDate, calculationType, status, offset, size);
        
        Long totalCount = calculationRequestRepository.countByContractWithFilters(
                contractId, fromDate, toDate, calculationType, status);
        
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
        CalculationRequestsResponse.Summary summary = buildCalculationSummary(
                contractId, fromDate, toDate, calculationType, status);
        
        return CalculationRequestsResponse.builder()
                .calculationRequests(requests)
                .pagination(pagination)
                .summary(summary)
                .build();
    }
    
    /**
     * Reconstruct CashFlowRequest from audit info
     */
    private CashFlowRequest reconstructCashFlowRequest(
            CalculationRequestsResponse.CalculationRequestInfo requestInfo) {
        
        // This is a simplified reconstruction - in a real implementation,
        // you would store the full request data in the audit trail
        return CashFlowRequest.builder()
                .requestId(requestInfo.getRequestId())
                .calculationType(CashFlowRequest.CalculationType.valueOf(requestInfo.getCalculationType()))
                .dateRange(CashFlowRequest.DateRange.builder()
                        .fromDate(LocalDate.parse(requestInfo.getFromDate()))
                        .toDate(LocalDate.parse(requestInfo.getToDate()))
                        .build())
                // Note: In a real implementation, you would need to store and retrieve
                // the full contract, position, lot, payment schedule, and market data
                .build();
    }
    
    /**
     * Perform integrity check
     */
    private CalculationReproduction.IntegrityCheck performIntegrityCheck(
            CalculationRequestsResponse.CalculationRequestInfo originalRequest,
            CashFlowResponse reproducedResults) {
        
        // Calculate hash of reproduced input data
        String reproducedHash = calculateInputDataHash(reproducedResults);
        
        // Check if hashes match
        boolean hashMatch = originalRequest.getInputDataHash().equals(reproducedHash);
        
        // For now, assume results match if hash matches
        // In a real implementation, you would compare the actual results
        boolean resultMatch = hashMatch;
        
        return CalculationReproduction.IntegrityCheck.builder()
                .inputDataHash(reproducedHash)
                .hashMatch(hashMatch)
                .resultMatch(resultMatch)
                .differences(hashMatch ? List.of() : List.of("Input data hash mismatch"))
                .build();
    }
    
    /**
     * Calculate input data hash
     */
    private String calculateInputDataHash(CashFlowResponse response) {
        // Simplified hash calculation - in reality, you would hash the input data
        return UUID.randomUUID().toString().substring(0, 16);
    }
    
    /**
     * Build calculation summary
     */
    private CalculationRequestsResponse.Summary buildCalculationSummary(
            String contractId, LocalDate fromDate, LocalDate toDate, 
            String calculationType, String status) {
        
        CalculationRequestsResponse.Summary.StatusBreakdown statusBreakdown = 
                CalculationRequestsResponse.Summary.StatusBreakdown.builder()
                        .processing(calculationRequestRepository.countByStatus("PROCESSING"))
                        .completed(calculationRequestRepository.countByStatus("COMPLETED"))
                        .failed(calculationRequestRepository.countByStatus("FAILED"))
                        .cancelled(calculationRequestRepository.countByStatus("CANCELLED"))
                        .build();
        
        Integer totalRequests = statusBreakdown.getProcessing() + statusBreakdown.getCompleted() + 
                statusBreakdown.getFailed() + statusBreakdown.getCancelled();
        
        return CalculationRequestsResponse.Summary.builder()
                .totalRequests(totalRequests)
                .statusBreakdown(statusBreakdown)
                .build();
    }
}
