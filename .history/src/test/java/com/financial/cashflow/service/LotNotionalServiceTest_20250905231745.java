package com.financial.cashflow.service;

import com.financial.cashflow.model.CashFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LotNotionalService
 */
@SpringBootTest
public class LotNotionalServiceTest {
    
    private LotNotionalService lotNotionalService;
    
    @BeforeEach
    void setUp() {
        lotNotionalService = new LotNotionalService();
    }
    
    @Test
    void testCalculateInterestBearingNotional_OnlySettledLots() {
        // Given
        String contractId = "EQ_SWAP_001";
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(150.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(155.0)
                .settlementDate(LocalDate.of(2024, 1, 22))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = lotNotionalService.calculateInterestBearingNotional(contractId, calculationDate, lots);
        
        // Then
        // Only LOT_001 should be considered (settled on 2024-01-12, before calculation date 2024-01-15)
        // LOT_002 is settled on 2024-01-22, after calculation date
        double expected = 1000.0 * 150.0; // Only LOT_001: quantity * costPrice
        assertEquals(expected, result, 0.01, "Only settled lots should contribute to notional");
    }
    
    @Test
    void testCalculateInterestBearingNotional_NoSettledLots() {
        // Given
        String contractId = "EQ_SWAP_001";
        LocalDate calculationDate = LocalDate.of(2024, 1, 10);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(150.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = lotNotionalService.calculateInterestBearingNotional(contractId, calculationDate, lots);
        
        // Then
        // No lots are settled before calculation date
        assertEquals(0.0, result, 0.01, "No notional should be calculated if no lots are settled");
    }
    
    @Test
    void testCalculateInterestBearingNotional_AllLotsSettled() {
        // Given
        String contractId = "EQ_SWAP_001";
        LocalDate calculationDate = LocalDate.of(2024, 1, 25);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(150.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(155.0)
                .settlementDate(LocalDate.of(2024, 1, 22))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = lotNotionalService.calculateInterestBearingNotional(contractId, calculationDate, lots);
        
        // Then
        // Both lots should be considered (both settled before calculation date)
        double expected = (1000.0 * 150.0) + (500.0 * 155.0); // LOT_001 + LOT_002
        assertEquals(expected, result, 0.01, "All settled lots should contribute to notional");
    }
    
    @Test
    void testCalculateInterestBearingNotional_ExcludesClosedLots() {
        // Given
        String contractId = "EQ_SWAP_001";
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(150.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(155.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.CLOSED)
                .build()
        );
        
        // When
        double result = lotNotionalService.calculateInterestBearingNotional(contractId, calculationDate, lots);
        
        // Then
        // Only LOT_001 should be considered (ACTIVE status), LOT_2 is CLOSED
        double expected = 1000.0 * 150.0; // Only LOT_001
        assertEquals(expected, result, 0.01, "Only active lots should contribute to notional");
    }
    
    @Test
    void testCalculateInterestBearingNotional_NoLotsForContract() {
        // Given
        String contractId = "EQ_SWAP_002";
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(150.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = lotNotionalService.calculateInterestBearingNotional(contractId, calculationDate, lots);
        
        // Then
        assertEquals(0.0, result, 0.01, "No notional should be calculated if no lots match the contract");
    }
    
    @Test
    void testCalculateInterestBearingNotionalForPeriod() {
        // Given
        String contractId = "EQ_SWAP_001";
        LocalDate startDate = LocalDate.of(2024, 1, 10);
        LocalDate endDate = LocalDate.of(2024, 1, 20);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(150.0)
                .settlementDate(LocalDate.of(2024, 1, 12))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(155.0)
                .settlementDate(LocalDate.of(2024, 1, 22))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = lotNotionalService.calculateInterestBearingNotionalForPeriod(contractId, startDate, endDate, lots);
        
        // Then
        // Only LOT_001 should be considered (settled on 2024-01-12, within the period)
        // LOT_002 is settled on 2024-01-22, outside the period
        double expected = 1000.0 * 150.0; // Only LOT_001
        assertEquals(expected, result, 0.01, "Only lots settled within the period should contribute to notional");
    }
}
