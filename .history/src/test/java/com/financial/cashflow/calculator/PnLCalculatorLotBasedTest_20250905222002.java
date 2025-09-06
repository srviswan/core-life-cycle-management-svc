package com.financial.cashflow.calculator;

import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for lot-based PnL calculation in PnLCalculator
 */
@SpringBootTest
public class PnLCalculatorLotBasedTest {
    
    private PnLCalculator pnLCalculator;
    
    @BeforeEach
    void setUp() {
        pnLCalculator = new PnLCalculator();
    }
    
    @Test
    void testLotBasedPnL_OnlyValidCostDates() {
        // Given
        CashFlowRequest.Contract contract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .notionalAmount(1000000.0)
                .build();
        
        MarketData marketData = MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(150.0)
                        .build())
                .build();
        
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(120.0)
                .costDate(LocalDate.of(2024, 1, 10)) // Valid cost date
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(130.0)
                .costDate(LocalDate.of(2024, 1, 20)) // Invalid cost date (after calculation date)
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = pnLCalculator.calculatePnL(contract, marketData, calculationDate, lots);
        
        // Then
        // Only LOT_001 should be considered: 1000 * (150 - 120) = 30,000
        assertEquals(30000.0, result, 0.01, "Only lots with valid cost dates should contribute to PnL");
    }
    
    @Test
    void testLotBasedPnL_AllValidCostDates() {
        // Given
        CashFlowRequest.Contract contract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .notionalAmount(1000000.0)
                .build();
        
        MarketData marketData = MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(150.0)
                        .build())
                .build();
        
        LocalDate calculationDate = LocalDate.of(2024, 1, 25);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(120.0)
                .costDate(LocalDate.of(2024, 1, 10))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(130.0)
                .costDate(LocalDate.of(2024, 1, 20))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = pnLCalculator.calculatePnL(contract, marketData, calculationDate, lots);
        
        // Then
        // Both lots should be considered: (1000 * (150 - 120)) + (500 * (150 - 130)) = 30,000 + 10,000 = 40,000
        assertEquals(40000.0, result, 0.01, "All lots with valid cost dates should contribute to PnL");
    }
    
    @Test
    void testLotBasedPnL_ExcludesClosedLots() {
        // Given
        CashFlowRequest.Contract contract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .notionalAmount(1000000.0)
                .build();
        
        MarketData marketData = MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(150.0)
                        .build())
                .build();
        
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(120.0)
                .costDate(LocalDate.of(2024, 1, 10))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build(),
            CashFlowRequest.Lot.builder()
                .lotId("LOT_002")
                .contractId("EQ_SWAP_001")
                .quantity(500.0)
                .costPrice(130.0)
                .costDate(LocalDate.of(2024, 1, 10))
                .status(CashFlowRequest.Lot.LotStatus.CLOSED) // Closed lot
                .build()
        );
        
        // When
        double result = pnLCalculator.calculatePnL(contract, marketData, calculationDate, lots);
        
        // Then
        // Only LOT_001 should be considered: 1000 * (150 - 120) = 30,000
        assertEquals(30000.0, result, 0.01, "Only active lots should contribute to PnL");
    }
    
    @Test
    void testLotBasedPnL_NoLotsForContract() {
        // Given
        CashFlowRequest.Contract contract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_002")
                .underlying("IBM")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .notionalAmount(1000000.0)
                .build();
        
        MarketData marketData = MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(150.0)
                        .build())
                .build();
        
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001") // Different contract
                .quantity(1000.0)
                .costPrice(120.0)
                .costDate(LocalDate.of(2024, 1, 10))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = pnLCalculator.calculatePnL(contract, marketData, calculationDate, lots);
        
        // Then
        // Should fall back to contract-based calculation
        // Contract notional: 1,000,000, base price: 100, current price: 150
        // PnL = 1,000,000 * (150 - 100) / 100 = 500,000
        assertEquals(500000.0, result, 0.01, "Should fall back to contract-based calculation when no lots match");
    }
    
    @Test
    void testLotBasedPnL_NoLotsProvided() {
        // Given
        CashFlowRequest.Contract contract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .notionalAmount(1000000.0)
                .build();
        
        MarketData marketData = MarketData.builder()
                .price(MarketData.PriceData.builder()
                        .symbol("IBM")
                        .basePrice(150.0)
                        .build())
                .build();
        
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        List<CashFlowRequest.Lot> lots = null;
        
        // When
        double result = pnLCalculator.calculatePnL(contract, marketData, calculationDate, lots);
        
        // Then
        // Should fall back to contract-based calculation
        assertEquals(500000.0, result, 0.01, "Should fall back to contract-based calculation when no lots provided");
    }
    
    @Test
    void testLotBasedPnL_NegativePnL() {
        // Given
        CashFlowRequest.Contract contract = CashFlowRequest.Contract.builder()
                .contractId("EQ_SWAP_001")
                .underlying("IBM")
                .type(CashFlowRequest.Contract.ContractType.EQUITY_SWAP)
                .notionalAmount(1000000.0)
                .build();
        
        MarketData marketData = MarketData.builder()
                .price(MarketData.SecurityData.builder()
                        .symbol("IBM")
                        .basePrice(100.0) // Lower than cost price
                        .build())
                .build();
        
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);
        
        List<CashFlowRequest.Lot> lots = Arrays.asList(
            CashFlowRequest.Lot.builder()
                .lotId("LOT_001")
                .contractId("EQ_SWAP_001")
                .quantity(1000.0)
                .costPrice(120.0)
                .costDate(LocalDate.of(2024, 1, 10))
                .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                .build()
        );
        
        // When
        double result = pnLCalculator.calculatePnL(contract, marketData, calculationDate, lots);
        
        // Then
        // Negative PnL: 1000 * (100 - 120) = -20,000
        assertEquals(-20000.0, result, 0.01, "Should handle negative PnL correctly");
    }
}
