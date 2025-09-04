package com.financial.cashflow.controller;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.CashFlowCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST API Controller for Cash Flow Management Service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cashflows")
@RequiredArgsConstructor
@Tag(name = "Cash Flow Management", description = "Cash flow calculation and management APIs")
public class CashFlowController {

    private final CashFlowCalculationService calculationService;

    @PostMapping("/calculate")
    @Operation(
        summary = "Calculate cash flows",
        description = "Calculate cash flows for a given contract and date range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cash flows calculated successfully",
            content = @Content(schema = @Schema(implementation = CashFlowResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public CompletableFuture<ResponseEntity<CashFlowResponse>> calculateCashFlows(
            @Valid @RequestBody CashFlowRequestContent request) {
        
        log.info("Received cash flow calculation request: {}", request.getRequestId());
        
        return calculationService.calculateCashFlows(request)
            .thenApply(response -> {
                log.info("Cash flow calculation completed for request: {}", request.getRequestId());
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                log.error("Error calculating cash flows for request: {}", request.getRequestId(), throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CashFlowResponse.builder()
                        .requestId(request.getRequestId())
                        .contractId(request.getContractId())
                        .status("FAILED")
                        .build());
            });
    }

    @PostMapping("/calculate/batch")
    @Operation(
        summary = "Calculate cash flows in batch",
        description = "Calculate cash flows for multiple contracts in batch"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch calculation completed successfully",
            content = @Content(schema = @Schema(implementation = BatchResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public CompletableFuture<ResponseEntity<BatchResponse>> calculateCashFlowsBatch(
            @Valid @RequestBody List<CashFlowRequestContent> requests) {
        
        log.info("Received batch cash flow calculation request for {} contracts", requests.size());
        
        return calculationService.calculateCashFlowsBatch(requests)
            .thenApply(responses -> {
                log.info("Batch cash flow calculation completed for {} contracts", requests.size());
                return ResponseEntity.ok(BatchResponse.builder()
                    .totalRequests(requests.size())
                    .successfulResponses(responses.size())
                    .responses(responses)
                    .build());
            })
            .exceptionally(throwable -> {
                log.error("Error in batch cash flow calculation", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BatchResponse.builder()
                        .totalRequests(requests.size())
                        .successfulResponses(0)
                        .errorMessage(throwable.getMessage())
                        .build());
            });
    }

    @PostMapping("/recalculate")
    @Operation(
        summary = "Recalculate cash flows",
        description = "Recalculate cash flows for a specific contract and date range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recalculation completed successfully",
            content = @Content(schema = @Schema(implementation = CashFlowResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public CompletableFuture<ResponseEntity<CashFlowResponse>> recalculateCashFlows(
            @Parameter(description = "Contract ID") @RequestParam String contractId,
            @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam String fromDate,
            @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam String toDate) {
        
        log.info("Received recalculation request for contract: {} from {} to {}", 
            contractId, fromDate, toDate);
        
        CashFlowRequestContent.DateRange dateRange = CashFlowRequestContent.DateRange.builder()
            .fromDate(LocalDate.parse(fromDate))
            .toDate(LocalDate.parse(toDate))
            .calculationFrequency("DAILY")
            .build();
        
        return calculationService.recalculateCashFlows(contractId, dateRange)
            .thenApply(response -> {
                log.info("Recalculation completed for contract: {}", contractId);
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                log.error("Error recalculating cash flows for contract: {}", contractId, throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CashFlowResponse.builder()
                        .requestId("RECALC_" + contractId)
                        .contractId(contractId)
                        .status("FAILED")
                        .build());
            });
    }

    @GetMapping("/cached/{requestId}")
    @Operation(
        summary = "Get cached calculation",
        description = "Retrieve a cached cash flow calculation result"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cached result found",
            content = @Content(schema = @Schema(implementation = CashFlowResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cached result not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<CashFlowResponse> getCachedCalculation(
            @Parameter(description = "Request ID") @PathVariable String requestId) {
        
        log.info("Retrieving cached calculation for request: {}", requestId);
        
        CashFlowResponse response = calculationService.getCachedCalculation(requestId);
        
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check the health status of the cash flow service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
        )
    })
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.builder()
            .status("UP")
            .timestamp(String.valueOf(System.currentTimeMillis()))
            .service("Cash Flow Management Service")
            .version("1.0.0")
            .build());
    }

    // Response DTOs

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchResponse {
        private int totalRequests;
        private int successfulResponses;
        private List<CashFlowResponse> responses;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String errorMessage;
        private String timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private String timestamp;
        private String service;
        private String version;
    }
}
