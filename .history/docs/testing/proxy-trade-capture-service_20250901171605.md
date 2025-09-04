# Proxy Lifecycle Management Service Specification

## Overview

The Proxy Lifecycle Management Service is designed to simulate the upstream lifecycle management system and generate realistic swap blotter data for testing the Cash Flow Management Service. It receives raw trades from Trade Capture, enriches them to swap blotters, and sends them to the Cash Flow Management Service. It can simulate 1M daily position updates with realistic patterns, including the 4PM peak activity.

## Service Architecture

### Core Components

```yaml
Proxy Lifecycle Management Service:
  Components:
    - Trade Enrichment Engine
    - Swap Blotter Generator
    - Position Management Engine
    - Lot Management Engine
    - Corporate Action Processor
    - Load Distribution Engine
    - Validation Engine
    - Monitoring & Metrics
  
  Interfaces:
    - REST API for swap blotter submission
    - Event streaming (Kafka) for real-time position updates
    - Batch file generation for bulk testing
    - Configuration management for test scenarios
```

### Service Design

```java
@Service
public class ProxyLifecycleManagementService {
    
    @Autowired
    private TradeEnrichmentEngine tradeEnrichment;
    
    @Autowired
    private SwapBlotterGenerator blotterGenerator;
    
    @Autowired
    private PositionManagementEngine positionManager;
    
    @Autowired
    private LoadDistributionEngine loadDistributor;
    
    @Autowired
    private ValidationEngine validator;
    
    @Autowired
    private MetricsCollector metrics;
    
    /**
     * Receive raw trades and enrich to swap blotters
     */
    public SwapBlotterGenerationResult processTrades(TradeEnrichmentRequest request) {
        // Receive raw trades from trade capture
        List<RawTrade> rawTrades = request.getRawTrades();
        
        // Enrich trades to swap blotters
        List<SwapBlotter> swapBlotters = tradeEnrichment.enrichTrades(rawTrades);
        
        // Update positions and lots
        List<Position> positions = positionManager.updatePositions(swapBlotters);
        List<Lot> lots = positionManager.updateLots(swapBlotters);
        
        // Distribute load according to pattern
        loadDistributor.distributeUpdates(swapBlotters, request.getLoadPattern());
        
        // Submit to cash flow service
        return submitToCashFlowService(swapBlotters, positions, lots);
    }
    
    /**
     * Simulate real-time lifecycle management
     */
    public void simulateRealTimeLifecycle(LifecycleSessionConfig config) {
        // Start real-time lifecycle simulation
        // Process trades according to market hours
        // Submit position updates with realistic timing
    }
}
```

## Trade Generation Engine

### Trade Types

```java
public enum TradeType {
    NEW_POSITION,      // 40% of trades
    POSITION_INCREASE, // 30% of trades
    POSITION_DECREASE, // 20% of trades
    LOT_CLOSURE        // 10% of trades
}

public enum TradeSize {
    SMALL(1, 100),     // 60% of trades
    MEDIUM(100, 1000), // 30% of trades
    LARGE(1000, 65000) // 10% of trades
}
```

### Trade Generation Logic

```java
@Component
public class TradeGeneratorEngine {
    
    public List<Trade> generateTrades(TradeGenerationRequest request) {
        List<Trade> trades = new ArrayList<>();
        
        for (int i = 0; i < request.getTradeCount(); i++) {
            Trade trade = generateTrade(i, request);
            trades.add(trade);
        }
        
        return trades;
    }
    
    private Trade generateTrade(int index, TradeGenerationRequest request) {
        TradeType tradeType = selectTradeType();
        TradeSize tradeSize = selectTradeSize();
        Contract contract = selectContract(request.getContractPool());
        
        return Trade.builder()
            .tradeId("TRADE_" + String.format("%06d", index))
            .contractId(contract.getContractId())
            .tradeType(tradeType)
            .quantity(generateQuantity(tradeSize))
            .price(generatePrice(contract.getUnderlying()))
            .tradeDate(generateTradeDate(request.getDateRange()))
            .tradeTime(generateTradeTime(request.getTradingHours()))
            .counterparty(selectCounterparty())
            .currency(contract.getCurrency())
            .build();
    }
    
    private TradeType selectTradeType() {
        double random = Math.random();
        if (random < 0.4) return TradeType.NEW_POSITION;
        if (random < 0.7) return TradeType.POSITION_INCREASE;
        if (random < 0.9) return TradeType.POSITION_DECREASE;
        return TradeType.LOT_CLOSURE;
    }
    
    private TradeSize selectTradeSize() {
        double random = Math.random();
        if (random < 0.6) return TradeSize.SMALL;
        if (random < 0.9) return TradeSize.MEDIUM;
        return TradeSize.LARGE;
    }
}
```

## Load Distribution Engine

### Trading Patterns

```yaml
Trading Patterns:
  Normal Trading Day:
    08:00-09:00: 5% of daily volume (Opening)
    09:00-11:00: 15% of daily volume (Morning session)
    11:00-13:00: 10% of daily volume (Lunch period)
    13:00-15:00: 20% of daily volume (Afternoon session)
    15:00-16:00: 30% of daily volume (Pre-close)
    16:00-17:00: 20% of daily volume (4PM peak)
  
  Peak Trading Pattern:
    15:00-16:00: 40% of daily volume
    16:00-17:00: 60% of daily volume (Peak)
  
  Extended Hours:
    17:00-20:00: 5% of daily volume (After hours)
    20:00-08:00: 0% of daily volume (Closed)
```

### Load Distribution Implementation

```java
@Component
public class LoadDistributionEngine {
    
    public void distributeTrades(List<Trade> trades, LoadPattern pattern) {
        Map<LocalTime, List<Trade>> timeSlots = new HashMap<>();
        
        // Distribute trades across time slots
        for (Trade trade : trades) {
            LocalTime tradeTime = calculateTradeTime(trade, pattern);
            timeSlots.computeIfAbsent(tradeTime, k -> new ArrayList<>()).add(trade);
        }
        
        // Submit trades with realistic timing
        submitTradesWithTiming(timeSlots, pattern);
    }
    
    private LocalTime calculateTradeTime(Trade trade, LoadPattern pattern) {
        double random = Math.random();
        
        switch (pattern) {
            case NORMAL_TRADING:
                return calculateNormalTradingTime(random);
            case PEAK_TRADING:
                return calculatePeakTradingTime(random);
            case STEADY_STATE:
                return calculateSteadyStateTime(random);
            default:
                return LocalTime.of(16, 0); // Default to 4PM
        }
    }
    
    private LocalTime calculatePeakTradingTime(double random) {
        if (random < 0.4) {
            // 15:00-16:00 (Pre-peak)
            return LocalTime.of(15, (int)(random * 60));
        } else {
            // 16:00-17:00 (Peak)
            return LocalTime.of(16, (int)((random - 0.4) * 60));
        }
    }
}
```

## Market Data Simulator

### Price Generation

```java
@Component
public class MarketDataSimulator {
    
    private final Map<String, PriceModel> priceModels = new HashMap<>();
    
    public double generatePrice(String underlying, LocalDateTime timestamp) {
        PriceModel model = priceModels.computeIfAbsent(underlying, this::createPriceModel);
        return model.generatePrice(timestamp);
    }
    
    private PriceModel createPriceModel(String underlying) {
        return PriceModel.builder()
            .basePrice(getBasePrice(underlying))
            .volatility(getVolatility(underlying))
            .trend(getTrend(underlying))
            .marketHours(getMarketHours())
            .build();
    }
    
    public static class PriceModel {
        private final double basePrice;
        private final double volatility;
        private final double trend;
        private final MarketHours marketHours;
        
        public double generatePrice(LocalDateTime timestamp) {
            // Generate realistic price movement
            double timeFactor = calculateTimeFactor(timestamp);
            double randomFactor = generateRandomFactor();
            double trendFactor = calculateTrendFactor(timestamp);
            
            return basePrice * (1 + trendFactor + randomFactor * volatility) * timeFactor;
        }
    }
}
```

## Trade Validation Engine

### Validation Rules

```java
@Component
public class TradeValidationEngine {
    
    public ValidationResult validateTrade(Trade trade) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Validate trade structure
        validateTradeStructure(trade, errors);
        
        // Validate business rules
        validateBusinessRules(trade, errors);
        
        // Validate market data
        validateMarketData(trade, errors);
        
        return ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .build();
    }
    
    private void validateTradeStructure(Trade trade, List<ValidationError> errors) {
        if (trade.getQuantity() <= 0) {
            errors.add(new ValidationError("INVALID_QUANTITY", "Quantity must be positive"));
        }
        
        if (trade.getPrice() <= 0) {
            errors.add(new ValidationError("INVALID_PRICE", "Price must be positive"));
        }
        
        if (trade.getTradeDate() == null) {
            errors.add(new ValidationError("MISSING_TRADE_DATE", "Trade date is required"));
        }
    }
    
    private void validateBusinessRules(Trade trade, List<ValidationError> errors) {
        // Check for duplicate trades
        if (isDuplicateTrade(trade)) {
            errors.add(new ValidationError("DUPLICATE_TRADE", "Trade already exists"));
        }
        
        // Check for market hours
        if (!isWithinMarketHours(trade.getTradeTime())) {
            errors.add(new ValidationError("OUTSIDE_MARKET_HOURS", "Trade outside market hours"));
        }
    }
}
```

## Configuration Management

### Test Scenarios

```yaml
Test Scenarios:
  Daily Load Test:
    tradeCount: 1000000
    duration: 24 hours
    loadPattern: NORMAL_TRADING
    peakMultiplier: 4.0
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN]
    
  Peak Load Test:
    tradeCount: 4000000
    duration: 2 hours
    loadPattern: PEAK_TRADING
    peakMultiplier: 8.0
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN]
    
  Stress Test:
    tradeCount: 10000000
    duration: 1 hour
    loadPattern: STEADY_STATE
    peakMultiplier: 10.0
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN, TSLA, NVDA, META, NFLX, ADBE]
    
  Historical Test:
    tradeCount: 5000000
    duration: 5 years
    loadPattern: HISTORICAL
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN]
```

### Configuration Schema

```java
@Configuration
public class TradeCaptureConfig {
    
    @Bean
    public TradeGenerationConfig tradeGenerationConfig() {
        return TradeGenerationConfig.builder()
            .defaultTradeCount(1000000)
            .defaultDuration(Duration.ofHours(24))
            .defaultLoadPattern(LoadPattern.NORMAL_TRADING)
            .defaultPeakMultiplier(4.0)
            .defaultContracts(Arrays.asList("IBM", "AAPL", "MSFT", "GOOGL", "AMZN"))
            .build();
    }
    
    @Bean
    public TradingHoursConfig tradingHoursConfig() {
        return TradingHoursConfig.builder()
            .marketOpen(LocalTime.of(8, 0))
            .marketClose(LocalTime.of(17, 0))
            .preMarketOpen(LocalTime.of(7, 0))
            .afterHoursClose(LocalTime.of(20, 0))
            .build();
    }
}
```

## API Endpoints

### Trade Generation API

```java
@RestController
@RequestMapping("/api/v1/trades")
public class TradeGenerationController {
    
    @PostMapping("/generate")
    public ResponseEntity<TradeGenerationResult> generateTrades(
            @RequestBody TradeGenerationRequest request) {
        
        TradeGenerationResult result = tradeCaptureService.generateTrades(request);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/simulate/realtime")
    public ResponseEntity<SimulationResult> simulateRealTimeTrading(
            @RequestBody TradingSessionConfig config) {
        
        SimulationResult result = tradeCaptureService.simulateRealTimeTrading(config);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/batch")
    public ResponseEntity<BatchResult> generateBatchTrades(
            @RequestBody BatchTradeRequest request) {
        
        BatchResult result = tradeCaptureService.generateBatchTrades(request);
        return ResponseEntity.ok(result);
    }
}
```

### Request/Response Models

```java
@Data
@Builder
public class TradeGenerationRequest {
    private int tradeCount;
    private Duration duration;
    private LoadPattern loadPattern;
    private double peakMultiplier;
    private List<String> contracts;
    private LocalDate startDate;
    private LocalDate endDate;
    private TradingHoursConfig tradingHours;
}

@Data
@Builder
public class TradeGenerationResult {
    private String sessionId;
    private int generatedTrades;
    private int submittedTrades;
    private int failedTrades;
    private Duration totalDuration;
    private Map<String, Integer> tradesByContract;
    private Map<LocalTime, Integer> tradesByHour;
    private List<ValidationError> errors;
    private PerformanceMetrics performance;
}
```

## Performance Monitoring

### Metrics Collection

```java
@Component
public class MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    public void recordTradeGeneration(Trade trade) {
        meterRegistry.counter("trades.generated", 
            "contract", trade.getContractId(),
            "type", trade.getTradeType().name(),
            "size", trade.getTradeSize().name())
            .increment();
    }
    
    public void recordTradeSubmission(Trade trade, Duration duration) {
        meterRegistry.timer("trades.submission.duration",
            "contract", trade.getContractId(),
            "type", trade.getTradeType().name())
            .record(duration);
    }
    
    public void recordLoadPattern(LoadPattern pattern, int tradeCount) {
        meterRegistry.gauge("trades.load.pattern",
            Tags.of("pattern", pattern.name()),
            tradeCount);
    }
}
```

### Performance Dashboard

```yaml
Performance Dashboard:
  Metrics:
    - Trades Generated per Second
    - Trades Submitted per Second
    - Average Submission Latency
    - Error Rate
    - Load Distribution by Hour
    - Contract Distribution
    - Trade Type Distribution
  
  Alerts:
    - High Error Rate (>1%)
    - Low Throughput (<1000 trades/second)
    - High Latency (>100ms)
    - Load Pattern Deviation
```

## Integration with Cash Flow Service

### Event Streaming Integration

```java
@Component
public class TradeEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, TradeEvent> kafkaTemplate;
    
    public void publishTradeEvent(Trade trade) {
        TradeEvent event = TradeEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tradeId(trade.getTradeId())
            .contractId(trade.getContractId())
            .eventType("TRADE_CAPTURED")
            .timestamp(Instant.now())
            .data(trade)
            .build();
        
        kafkaTemplate.send("trade-events", trade.getContractId(), event);
    }
}
```

### REST API Integration

```java
@Component
public class CashFlowServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public CashFlowResponse submitTradeForCalculation(Trade trade) {
        CashFlowRequest request = buildCashFlowRequest(trade);
        
        return restTemplate.postForObject(
            "/cashflows/calculate",
            request,
            CashFlowResponse.class
        );
    }
    
    private CashFlowRequest buildCashFlowRequest(Trade trade) {
        // Build cash flow request from trade data
        return CashFlowRequest.builder()
            .requestId("CF_REQ_" + trade.getTradeId())
            .contractId(trade.getContractId())
            .dateRange(buildDateRange(trade.getTradeDate()))
            .contracts(buildContractData(trade))
            .positions(buildPositionData(trade))
            .lots(buildLotData(trade))
            .marketData(buildMarketData(trade))
            .build();
    }
}
```

This comprehensive proxy trade capture service specification provides the foundation for simulating realistic trading activity and validating the Cash Flow Management Service under various load conditions.
