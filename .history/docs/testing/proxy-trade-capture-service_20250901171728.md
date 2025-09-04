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

## Trade Enrichment Engine

### Swap Blotter Types

```java
public enum SwapBlotterType {
    NEW_SWAP_CONTRACT,    // 30% of blotters
    POSITION_INCREASE,     // 35% of blotters
    POSITION_DECREASE,     // 25% of blotters
    CORPORATE_ACTION,      // 5% of blotters
    LOT_CLOSURE           // 5% of blotters
}

public enum PositionSize {
    SMALL(1, 1000),       // 60% of positions
    MEDIUM(1000, 10000),  // 30% of positions
    LARGE(10000, 65000)   // 10% of positions
}
```

### Trade Enrichment Logic

```java
@Component
public class TradeEnrichmentEngine {
    
    public List<SwapBlotter> enrichTrades(List<RawTrade> rawTrades) {
        List<SwapBlotter> swapBlotters = new ArrayList<>();
        
        for (RawTrade rawTrade : rawTrades) {
            SwapBlotter blotter = enrichTrade(rawTrade);
            swapBlotters.add(blotter);
        }
        
        return swapBlotters;
    }
    
    private SwapBlotter enrichTrade(RawTrade rawTrade) {
        SwapBlotterType blotterType = determineBlotterType(rawTrade);
        PositionSize positionSize = determinePositionSize(rawTrade);
        Contract contract = enrichContract(rawTrade);
        
        return SwapBlotter.builder()
            .blotterId("BLOTTER_" + String.format("%06d", rawTrade.getTradeId()))
            .contractId(contract.getContractId())
            .blotterType(blotterType)
            .underlying(contract.getUnderlying())
            .quantity(rawTrade.getQuantity())
            .notionalAmount(calculateNotional(rawTrade))
            .tradeDate(rawTrade.getTradeDate())
            .tradeTime(rawTrade.getTradeTime())
            .equityLeg(buildEquityLeg(contract, rawTrade))
            .interestLeg(buildInterestLeg(contract, rawTrade))
            .currency(contract.getCurrency())
            .build();
    }
    
    private SwapBlotterType determineBlotterType(RawTrade rawTrade) {
        double random = Math.random();
        if (random < 0.3) return SwapBlotterType.NEW_SWAP_CONTRACT;
        if (random < 0.65) return SwapBlotterType.POSITION_INCREASE;
        if (random < 0.9) return SwapBlotterType.POSITION_DECREASE;
        if (random < 0.95) return SwapBlotterType.CORPORATE_ACTION;
        return SwapBlotterType.LOT_CLOSURE;
    }
    
    private PositionSize determinePositionSize(RawTrade rawTrade) {
        int quantity = rawTrade.getQuantity();
        if (quantity <= 1000) return PositionSize.SMALL;
        if (quantity <= 10000) return PositionSize.MEDIUM;
        return PositionSize.LARGE;
    }
}
```

## Load Distribution Engine

### Lifecycle Update Patterns

```yaml
Lifecycle Update Patterns:
  Normal Trading Day:
    08:00-09:00: 5% of daily updates (Opening)
    09:00-11:00: 15% of daily updates (Morning session)
    11:00-13:00: 10% of daily updates (Lunch period)
    13:00-15:00: 20% of daily updates (Afternoon session)
    15:00-16:00: 30% of daily updates (Pre-close)
    16:00-17:00: 20% of daily updates (4PM peak)
  
  Peak Update Pattern:
    15:00-16:00: 40% of daily updates
    16:00-17:00: 60% of daily updates (Peak)
  
  Extended Hours:
    17:00-20:00: 5% of daily updates (After hours)
    20:00-08:00: 0% of daily updates (Closed)
```

### Load Distribution Implementation

```java
@Component
public class LoadDistributionEngine {
    
    public void distributeUpdates(List<SwapBlotter> swapBlotters, LoadPattern pattern) {
        Map<LocalTime, List<SwapBlotter>> timeSlots = new HashMap<>();
        
        // Distribute updates across time slots
        for (SwapBlotter blotter : swapBlotters) {
            LocalTime updateTime = calculateUpdateTime(blotter, pattern);
            timeSlots.computeIfAbsent(updateTime, k -> new ArrayList<>()).add(blotter);
        }
        
        // Submit updates with realistic timing
        submitUpdatesWithTiming(timeSlots, pattern);
    }
    
    private LocalTime calculateUpdateTime(SwapBlotter blotter, LoadPattern pattern) {
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

## Position Management Engine

### Position Update Logic

```java
@Component
public class PositionManagementEngine {
    
    private final Map<String, Position> positions = new HashMap<>();
    private final Map<String, List<Lot>> lots = new HashMap<>();
    
    public List<Position> updatePositions(List<SwapBlotter> swapBlotters) {
        List<Position> updatedPositions = new ArrayList<>();
        
        for (SwapBlotter blotter : swapBlotters) {
            Position position = updatePosition(blotter);
            updatedPositions.add(position);
        }
        
        return updatedPositions;
    }
    
    public List<Lot> updateLots(List<SwapBlotter> swapBlotters) {
        List<Lot> updatedLots = new ArrayList<>();
        
        for (SwapBlotter blotter : swapBlotters) {
            List<Lot> contractLots = updateLotsForContract(blotter);
            updatedLots.addAll(contractLots);
        }
        
        return updatedLots;
    }
    
    private Position updatePosition(SwapBlotter blotter) {
        String positionId = "POS_" + blotter.getContractId();
        Position position = positions.get(positionId);
        
        if (position == null) {
            position = createNewPosition(blotter);
        } else {
            position = updateExistingPosition(position, blotter);
        }
        
        positions.put(positionId, position);
        return position;
    }
    
    private Position createNewPosition(SwapBlotter blotter) {
        return Position.builder()
            .positionId("POS_" + blotter.getContractId())
            .contractId(blotter.getContractId())
            .underlying(blotter.getUnderlying())
            .totalQuantity(blotter.getQuantity())
            .currency(blotter.getCurrency())
            .status("ACTIVE")
            .build();
    }
    
    private Position updateExistingPosition(Position position, SwapBlotter blotter) {
        int newQuantity = position.getTotalQuantity() + blotter.getQuantity();
        
        return Position.builder()
            .positionId(position.getPositionId())
            .contractId(position.getContractId())
            .underlying(position.getUnderlying())
            .totalQuantity(newQuantity)
            .currency(position.getCurrency())
            .status(newQuantity > 0 ? "ACTIVE" : "CLOSED")
            .build();
    }
}
```

## Validation Engine

### Validation Rules

```java
@Component
public class ValidationEngine {
    
    public ValidationResult validateSwapBlotter(SwapBlotter blotter) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Validate blotter structure
        validateBlotterStructure(blotter, errors);
        
        // Validate business rules
        validateBusinessRules(blotter, errors);
        
        // Validate contract consistency
        validateContractConsistency(blotter, errors);
        
        return ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .build();
    }
    
    private void validateBlotterStructure(SwapBlotter blotter, List<ValidationError> errors) {
        if (blotter.getQuantity() <= 0) {
            errors.add(new ValidationError("INVALID_QUANTITY", "Quantity must be positive"));
        }
        
        if (blotter.getNotionalAmount() <= 0) {
            errors.add(new ValidationError("INVALID_NOTIONAL", "Notional amount must be positive"));
        }
        
        if (blotter.getTradeDate() == null) {
            errors.add(new ValidationError("MISSING_TRADE_DATE", "Trade date is required"));
        }
    }
    
    private void validateBusinessRules(SwapBlotter blotter, List<ValidationError> errors) {
        // Check for duplicate blotters
        if (isDuplicateBlotter(blotter)) {
            errors.add(new ValidationError("DUPLICATE_BLOTTER", "Blotter already exists"));
        }
        
        // Check for market hours
        if (!isWithinMarketHours(blotter.getTradeTime())) {
            errors.add(new ValidationError("OUTSIDE_MARKET_HOURS", "Trade outside market hours"));
        }
    }
    
    private void validateContractConsistency(SwapBlotter blotter, List<ValidationError> errors) {
        // Validate equity leg
        if (blotter.getEquityLeg() == null) {
            errors.add(new ValidationError("MISSING_EQUITY_LEG", "Equity leg is required"));
        }
        
        // Validate interest leg
        if (blotter.getInterestLeg() == null) {
            errors.add(new ValidationError("MISSING_INTEREST_LEG", "Interest leg is required"));
        }
        
        // Validate currency consistency
        if (!blotter.getCurrency().equals(blotter.getEquityLeg().getCurrency()) ||
            !blotter.getCurrency().equals(blotter.getInterestLeg().getCurrency())) {
            errors.add(new ValidationError("CURRENCY_MISMATCH", "Currency mismatch between legs"));
        }
    }
}
```

## Configuration Management

### Test Scenarios

```yaml
Test Scenarios:
  Daily Load Test:
    positionUpdateCount: 1000000
    duration: 24 hours
    loadPattern: NORMAL_TRADING
    peakMultiplier: 4.0
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN]
    
  Peak Load Test:
    positionUpdateCount: 4000000
    duration: 2 hours
    loadPattern: PEAK_TRADING
    peakMultiplier: 8.0
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN]
    
  Stress Test:
    positionUpdateCount: 10000000
    duration: 1 hour
    loadPattern: STEADY_STATE
    peakMultiplier: 10.0
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN, TSLA, NVDA, META, NFLX, ADBE]
    
  Historical Test:
    positionUpdateCount: 5000000
    duration: 5 years
    loadPattern: HISTORICAL
    contracts: [IBM, AAPL, MSFT, GOOGL, AMZN]
```

### Configuration Schema

```java
@Configuration
public class LifecycleManagementConfig {
    
    @Bean
    public LifecycleGenerationConfig lifecycleGenerationConfig() {
        return LifecycleGenerationConfig.builder()
            .defaultPositionUpdateCount(1000000)
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

### Lifecycle Management API

```java
@RestController
@RequestMapping("/api/v1/lifecycle")
public class LifecycleManagementController {
    
    @PostMapping("/process-trades")
    public ResponseEntity<SwapBlotterGenerationResult> processTrades(
            @RequestBody TradeEnrichmentRequest request) {
        
        SwapBlotterGenerationResult result = lifecycleManagementService.processTrades(request);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/simulate/realtime")
    public ResponseEntity<SimulationResult> simulateRealTimeLifecycle(
            @RequestBody LifecycleSessionConfig config) {
        
        SimulationResult result = lifecycleManagementService.simulateRealTimeLifecycle(config);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/batch")
    public ResponseEntity<BatchResult> generateBatchUpdates(
            @RequestBody BatchLifecycleRequest request) {
        
        BatchResult result = lifecycleManagementService.generateBatchUpdates(request);
        return ResponseEntity.ok(result);
    }
}
```

### Request/Response Models

```java
@Data
@Builder
public class TradeEnrichmentRequest {
    private List<RawTrade> rawTrades;
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
public class SwapBlotterGenerationResult {
    private String sessionId;
    private int processedTrades;
    private int generatedBlotters;
    private int submittedUpdates;
    private int failedUpdates;
    private Duration totalDuration;
    private Map<String, Integer> updatesByContract;
    private Map<LocalTime, Integer> updatesByHour;
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
    
    public void recordBlotterGeneration(SwapBlotter blotter) {
        meterRegistry.counter("blotters.generated", 
            "contract", blotter.getContractId(),
            "type", blotter.getBlotterType().name(),
            "size", blotter.getPositionSize().name())
            .increment();
    }
    
    public void recordPositionUpdate(Position position, Duration duration) {
        meterRegistry.timer("positions.update.duration",
            "contract", position.getContractId(),
            "status", position.getStatus())
            .record(duration);
    }
    
    public void recordLoadPattern(LoadPattern pattern, int updateCount) {
        meterRegistry.gauge("lifecycle.load.pattern",
            Tags.of("pattern", pattern.name()),
            updateCount);
    }
}
```

### Performance Dashboard

```yaml
Performance Dashboard:
  Metrics:
    - Blotters Generated per Second
    - Position Updates per Second
    - Average Update Latency
    - Error Rate
    - Load Distribution by Hour
    - Contract Distribution
    - Blotter Type Distribution
  
  Alerts:
    - High Error Rate (>1%)
    - Low Throughput (<1000 updates/second)
    - High Latency (>100ms)
    - Load Pattern Deviation
```

## Integration with Cash Flow Service

### Event Streaming Integration

```java
@Component
public class LifecycleEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, LifecycleEvent> kafkaTemplate;
    
    public void publishLifecycleEvent(SwapBlotter blotter) {
        LifecycleEvent event = LifecycleEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .blotterId(blotter.getBlotterId())
            .contractId(blotter.getContractId())
            .eventType("POSITION_UPDATED")
            .timestamp(Instant.now())
            .data(blotter)
            .build();
        
        kafkaTemplate.send("lifecycle-events", blotter.getContractId(), event);
    }
}
```

### REST API Integration

```java
@Component
public class CashFlowServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public CashFlowResponse submitBlotterForCalculation(SwapBlotter blotter, List<Position> positions, List<Lot> lots) {
        CashFlowRequest request = buildCashFlowRequest(blotter, positions, lots);
        
        return restTemplate.postForObject(
            "/cashflows/calculate",
            request,
            CashFlowResponse.class
        );
    }
    
    private CashFlowRequest buildCashFlowRequest(SwapBlotter blotter, List<Position> positions, List<Lot> lots) {
        // Build cash flow request from swap blotter data
        return CashFlowRequest.builder()
            .requestId("CF_REQ_" + blotter.getBlotterId())
            .contractId(blotter.getContractId())
            .dateRange(buildDateRange(blotter.getTradeDate()))
            .contracts(buildContractData(blotter))
            .positions(positions)
            .lots(lots)
            .paymentSchedules(buildPaymentSchedules(blotter))
            .marketDataStrategy(buildMarketDataStrategy())
            .marketData(buildMarketData(blotter))
            .build();
    }
}
```

This comprehensive proxy lifecycle management service specification provides the foundation for simulating realistic lifecycle management activity and validating the Cash Flow Management Service under various load conditions.
