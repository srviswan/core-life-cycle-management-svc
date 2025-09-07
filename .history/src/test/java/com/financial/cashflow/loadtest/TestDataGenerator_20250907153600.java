package com.financial.cashflow.loadtest;

import com.financial.cashflow.model.CashFlowRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test Data Generator for Load Testing
 * Creates various test scenarios with different complexity levels
 */
@Slf4j
public class TestDataGenerator {
    
    /**
     * Generate simple test request (1 contract, 1 position, 1 lot)
     */
    public static CashFlowRequest generateSimpleRequest() {
        return generateRequest(1, 1, 1, "SIMPLE");
    }
    
    /**
     * Generate medium complexity test request (2 contracts, 2 positions each, 3 lots each)
     */
    public static CashFlowRequest generateMediumRequest() {
        return generateRequest(2, 2, 3, "MEDIUM");
    }
    
    /**
     * Generate complex test request (3 contracts, 3 positions each, 5 lots each)
     */
    public static CashFlowRequest generateComplexRequest() {
        return generateRequest(3, 3, 5, "COMPLEX");
    }
    
    /**
     * Generate high complexity test request (5 contracts, 4 positions each, 8 lots each)
     */
    public static CashFlowRequest generateHighComplexityRequest() {
        return generateRequest(5, 4, 8, "HIGH_COMPLEXITY");
    }
    
    /**
     * Generate extreme complexity test request (10 contracts, 5 positions each, 10 lots each)
     */
    public static CashFlowRequest generateExtremeRequest() {
        return generateRequest(10, 5, 10, "EXTREME");
    }
    
    /**
     * Generate test request with specified complexity
     */
    public static CashFlowRequest generateRequest(int contractCount, int positionsPerContract, 
                                                int lotsPerPosition, String complexity) {
        String requestId = String.format("LOAD_TEST_%s_%d_%d_%d_%s", 
            complexity, contractCount, positionsPerContract, lotsPerPosition, 
            UUID.randomUUID().toString().substring(0, 8));
        
        List<CashFlowRequest.ContractPosition> contractPositions = new ArrayList<>();
        
        for (int c = 0; c < contractCount; c++) {
            String contractId = String.format("CONTRACT_%s_%d", complexity, c + 1);
            String underlying = getUnderlyingSymbol(c);
            
            List<CashFlowRequest.Position> positions = new ArrayList<>();
            
            for (int p = 0; p < positionsPerContract; p++) {
                String positionId = String.format("POS_%s_%d_%d", complexity, c + 1, p + 1);
                String positionType = getPositionType(p);
                
                List<CashFlowRequest.Lot> lots = new ArrayList<>();
                
                for (int l = 0; l < lotsPerPosition; l++) {
                    String lotId = String.format("LOT_%s_%d_%d_%d", complexity, c + 1, p + 1, l + 1);
                    
                    CashFlowRequest.Lot lot = CashFlowRequest.Lot.builder()
                        .lotId(lotId)
                        .contractId(contractId)
                        .positionId(positionId)
                        .underlying(underlying)
                        .quantity(100.0 + (l * 50.0)) // Varying quantities
                        .costPrice(100.0 + (l * 5.0)) // Varying cost prices
                        .costDate(LocalDate.now().minusDays(l))
                        .settlementDate(LocalDate.now().plusDays(l))
                        .lotType(CashFlowRequest.Lot.LotType.NEW_LOT)
                        .status(CashFlowRequest.Lot.LotStatus.ACTIVE)
                        .unwindingMethod("FIFO")
                        .build();
                    
                    lots.add(lot);
                }
                
                CashFlowRequest.Position position = CashFlowRequest.Position.builder()
                    .positionId(positionId)
                    .product(getProductName(p))
                    .underlying(underlying)
                    .notionalAmount(1000000.0 + (p * 500000.0))
                    .currency("USD")
                    .type(getPositionTypeEnum(p))
                    .lots(lots)
                    .build();
                
                positions.add(position);
            }
            
            CashFlowRequest.ContractPosition contractPosition = CashFlowRequest.ContractPosition.builder()
                .contractId(contractId)
                .underlying(underlying)
                .index("SOFR")
                .type(CashFlowRequest.ContractPosition.ContractType.EQUITY_SWAP)
                .currency("USD")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().plusDays(365))
                .totalNotionalAmount(2000000.0 + (c * 1000000.0))
                .positions(positions)
                .build();
            
            contractPositions.add(contractPosition);
        }
        
        return CashFlowRequest.builder()
            .requestId(requestId)
            .calculationDate(LocalDate.now())
            .dateRange(CashFlowRequest.DateRange.builder()
                .fromDate(LocalDate.now().minusDays(30))
                .toDate(LocalDate.now().plusDays(30))
                .build())
            .calculationType(CashFlowRequest.CalculationType.REAL_TIME_PROCESSING)
            .marketDataStrategy(CashFlowRequest.MarketDataStrategy.builder()
                .mode(CashFlowRequest.MarketDataStrategy.Mode.SELF_CONTAINED)
                .build())
            .marketData(createMarketData())
            .contractPositions(contractPositions)
            .build();
    }
    
    /**
     * Create market data for testing
     */
    private static CashFlowRequest.MarketDataContainer createMarketData() {
        List<CashFlowRequest.Security> securities = new ArrayList<>();
        List<CashFlowRequest.Rate> rates = new ArrayList<>();
        List<CashFlowRequest.Dividend> dividends = new ArrayList<>();
        
        // Create securities for different underlyings
        String[] symbols = {"IBM", "AAPL", "MSFT", "GOOGL", "TSLA", "AMZN", "META", "NVDA", "NFLX", "AMD"};
        
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i];
            
            // Security
            List<CashFlowRequest.PriceChange> priceChanges = new ArrayList<>();
            priceChanges.add(CashFlowRequest.PriceChange.builder()
                .date(LocalDate.now())
                .price(100.0 + (i * 10.0))
                .build());
            
            CashFlowRequest.Security security = CashFlowRequest.Security.builder()
                .symbol(symbol)
                .basePrice(100.0 + (i * 10.0))
                .baseDate(LocalDate.now().minusDays(1))
                .changes(priceChanges)
                .build();
            
            securities.add(security);
            
            // Dividend
            List<CashFlowRequest.DividendInfo> dividendInfos = new ArrayList<>();
            dividendInfos.add(CashFlowRequest.DividendInfo.builder()
                .exDate(LocalDate.now().minusDays(5))
                .amount(1.0 + (i * 0.1))
                .currency("USD")
                .build());
            
            CashFlowRequest.Dividend dividend = CashFlowRequest.Dividend.builder()
                .symbol(symbol)
                .dividends(dividendInfos)
                .build();
            
            dividends.add(dividend);
        }
        
        // Rate
        List<CashFlowRequest.RateChange> rateChanges = new ArrayList<>();
        rateChanges.add(CashFlowRequest.RateChange.builder()
            .date(LocalDate.now())
            .rate(5.25)
            .build());
        
        CashFlowRequest.Rate rate = CashFlowRequest.Rate.builder()
            .index("SOFR")
            .baseRate(5.25)
            .baseDate(LocalDate.now().minusDays(1))
            .changes(rateChanges)
            .build();
        
        rates.add(rate);
        
        CashFlowRequest.MarketDataContent content = CashFlowRequest.MarketDataContent.builder()
            .securities(securities)
            .rates(rates)
            .dividends(dividends)
            .build();
        
        return CashFlowRequest.MarketDataContainer.builder()
            .data(content)
            .build();
    }
    
    private static String getUnderlyingSymbol(int index) {
        String[] symbols = {"IBM", "AAPL", "MSFT", "GOOGL", "TSLA", "AMZN", "META", "NVDA", "NFLX", "AMD"};
        return symbols[index % symbols.length];
    }
    
    private static String getPositionType(int index) {
        String[] types = {"EQUITY_LEG", "INTEREST_LEG", "DIVIDEND_LEG", "CURRENCY_LEG"};
        return types[index % types.length];
    }
    
    private static CashFlowRequest.Position.PositionType getPositionTypeEnum(int index) {
        CashFlowRequest.Position.PositionType[] types = {
            CashFlowRequest.Position.PositionType.EQUITY_LEG,
            CashFlowRequest.Position.PositionType.INTEREST_LEG,
            CashFlowRequest.Position.PositionType.DIVIDEND_LEG,
            CashFlowRequest.Position.PositionType.CURRENCY_LEG
        };
        return types[index % types.length];
    }
    
    private static String getProductName(int index) {
        String[] products = {"EQUITY_LEG", "INTEREST_LEG", "DIVIDEND_LEG", "CURRENCY_LEG"};
        return products[index % products.length];
    }
}
