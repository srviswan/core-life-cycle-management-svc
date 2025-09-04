# Unified Cash Flow Engine Implementation Guidelines

## Overview

The Unified Cash Flow Engine is a core component of the Cash Flow Service designed to handle both historical recalculation and real-time processing for synthetic swap contracts. It supports three calculation modes:

1. **Historical Recalculation Mode**: Large-scale historical calculations (5+ years) with sub-5-minute performance
2. **Real-Time Processing Mode**: Single-day calculations with sub-100ms latency
3. **Incremental Update Mode**: Medium-range calculations (2-30 days) with balanced performance

## Architecture Principles

### 1. Mode-Based Processing
- **Historical Mode**: Memory-efficient chunking for large date ranges
- **Real-Time Mode**: In-memory processing for single-day calculations
- **Incremental Mode**: Hybrid approach for medium-range calculations
- **Automatic Mode Selection**: Intelligent routing based on request characteristics

### 2. Unified Data Model
- **Common Calculation Logic**: Shared algorithms across all modes
- **Mode-Specific Optimizations**: Tailored performance strategies
- **Consistent Results**: Same mathematical accuracy regardless of mode
- **Shared Components**: Reuse of core calculation components

### 3. Performance Optimization
- **Historical Mode**: Parallel processing and memory management
- **Real-Time Mode**: Low-latency in-memory operations
- **Incremental Mode**: Balanced approach with caching
- **Adaptive Processing**: Dynamic optimization based on workload

### 4. Deterministic Results
- **Ordered Processing**: Ensure consistent event ordering
- **Reproducible Calculations**: Same inputs always produce same outputs
- **Audit Trail**: Complete logging of all calculation steps
- **Version Control**: Track calculation algorithm versions

## Core Components

### 1. Data Ingestion Layer

```java
@Component
public class HistoricalDataIngestionService {
    
    public HistoricalDataContainer ingestData(CashFlowRequest request) {
        return HistoricalDataContainer.builder()
            .contracts(loadContracts(request.getContracts()))
            .marketData(loadMarketData(request.getHistoricalData().getMarketData()))
            .positions(loadPositions(request.getHistoricalData().getPositions()))
            .corporateActions(loadCorporateActions(request.getHistoricalData().getCorporateActions()))
            .tradeEvents(loadTradeEvents(request.getHistoricalData().getTradeEvents()))
            .build();
    }
    
    private MarketDataContainer loadMarketData(MarketDataRequest marketData) {
        return MarketDataContainer.builder()
            .securities(createTimeSeriesMap(marketData.getSecurities()))
            .rates(createTimeSeriesMap(marketData.getRates()))
            .dividends(createTimeSeriesMap(marketData.getDividends()))
            .build();
    }
}
```

### 2. Time Series Data Structure

```java
public class TimeSeriesMap<K, V> {
    private final NavigableMap<LocalDate, Map<K, V>> dataByDate;
    private final Map<K, NavigableMap<LocalDate, V>> dataByKey;
    
    public V getValue(K key, LocalDate date) {
        NavigableMap<LocalDate, V> timeSeries = dataByKey.get(key);
        if (timeSeries == null) return null;
        
        Map.Entry<LocalDate, V> entry = timeSeries.floorEntry(date);
        return entry != null ? entry.getValue() : null;
    }
    
    public List<V> getValuesInRange(K key, LocalDate fromDate, LocalDate toDate) {
        NavigableMap<LocalDate, V> timeSeries = dataByKey.get(key);
        if (timeSeries == null) return Collections.emptyList();
        
        return timeSeries.subMap(fromDate, true, toDate, true)
                        .values()
                        .stream()
                        .collect(Collectors.toList());
    }
}
```

### 3. Calculation Engine

```java
@Component
public class HistoricalCalculationEngine {
    
    private final CalculationStrategyFactory strategyFactory;
    private final ParallelExecutionService parallelService;
    private final MemoryManager memoryManager;
    
    public CalculationResult calculate(HistoricalDataContainer data, DateRange dateRange) {
        try {
            // Pre-allocate memory pools
            memoryManager.allocateCalculationPools();
            
            // Process contracts in parallel
            List<ContractCalculationTask> tasks = createCalculationTasks(data, dateRange);
            List<ContractResult> contractResults = parallelService.executeParallel(tasks);
            
            // Aggregate results
            return aggregateResults(contractResults, dateRange);
            
        } finally {
            // Clean up memory
            memoryManager.releaseCalculationPools();
        }
    }
    
    private List<ContractCalculationTask> createCalculationTasks(
            HistoricalDataContainer data, DateRange dateRange) {
        
        return data.getContracts().stream()
            .map(contract -> new ContractCalculationTask(contract, data, dateRange))
            .collect(Collectors.toList());
    }
}
```

### 4. Contract-Level Calculation Task

```java
public class ContractCalculationTask implements Callable<ContractResult> {
    
    private final SwapContract contract;
    private final HistoricalDataContainer data;
    private final DateRange dateRange;
    
    @Override
    public ContractResult call() {
        // Process equity and interest legs in parallel
        CompletableFuture<EquityLegResult> equityFuture = 
            CompletableFuture.supplyAsync(() -> calculateEquityLeg());
        CompletableFuture<InterestLegResult> interestFuture = 
            CompletableFuture.supplyAsync(() -> calculateInterestLeg());
        
        // Wait for both legs to complete
        CompletableFuture.allOf(equityFuture, interestFuture).join();
        
        return ContractResult.builder()
            .contractId(contract.getContractId())
            .equityLegResult(equityFuture.get())
            .interestLegResult(interestFuture.get())
            .build();
    }
    
    private EquityLegResult calculateEquityLeg() {
        List<LocalDate> calculationDates = generateCalculationDates(dateRange);
        
        return calculationDates.parallelStream()
            .map(this::calculateDailyEquityCashFlow)
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                this::aggregateEquityResults
            ));
    }
}
```

## Performance Optimization Strategies

### 1. Date Range Optimization

```java
@Component
public class DateRangeOptimizer {
    
    public List<DateRange> optimizeDateRanges(DateRange originalRange) {
        List<DateRange> optimizedRanges = new ArrayList<>();
        
        // Split large ranges into smaller chunks
        if (originalRange.getDays() > 1000) {
            optimizedRanges.addAll(splitIntoChunks(originalRange, 1000));
        } else {
            optimizedRanges.add(originalRange);
        }
        
        // Merge adjacent ranges with similar characteristics
        return mergeSimilarRanges(optimizedRanges);
    }
    
    private List<DateRange> splitIntoChunks(DateRange range, int maxDays) {
        List<DateRange> chunks = new ArrayList<>();
        LocalDate currentDate = range.getFromDate();
        
        while (currentDate.isBefore(range.getToDate())) {
            LocalDate endDate = currentDate.plusDays(maxDays - 1);
            if (endDate.isAfter(range.getToDate())) {
                endDate = range.getToDate();
            }
            
            chunks.add(new DateRange(currentDate, endDate));
            currentDate = endDate.plusDays(1);
        }
        
        return chunks;
    }
}
```

### 2. Memory Management

```java
@Component
public class MemoryManager {
    
    private final ObjectPool<CashFlowCalculation> calculationPool;
    private final ObjectPool<MarketDataPoint> marketDataPool;
    private final ObjectPool<PositionSnapshot> positionPool;
    
    public void allocateCalculationPools() {
        calculationPool.allocate(1000);
        marketDataPool.allocate(10000);
        positionPool.allocate(5000);
    }
    
    public CashFlowCalculation borrowCalculation() {
        return calculationPool.borrow();
    }
    
    public void returnCalculation(CashFlowCalculation calculation) {
        calculation.reset();
        calculationPool.returnObject(calculation);
    }
    
    public void releaseCalculationPools() {
        calculationPool.clear();
        marketDataPool.clear();
        positionPool.clear();
    }
}
```

### 3. Caching Strategy

```java
@Component
public class CalculationCache {
    
    private final Cache<String, CalculationResult> resultCache;
    private final Cache<String, MarketDataSnapshot> marketDataCache;
    
    public CalculationResult getCachedResult(String cacheKey) {
        return resultCache.getIfPresent(cacheKey);
    }
    
    public void cacheResult(String cacheKey, CalculationResult result) {
        resultCache.put(cacheKey, result);
    }
    
    public String generateCacheKey(ContractCalculationTask task) {
        return String.format("%s_%s_%s_%s",
            task.getContract().getContractId(),
            task.getDateRange().getFromDate(),
            task.getDateRange().getToDate(),
            task.getCalculationVersion());
    }
}
```

## Calculation Algorithms

### 1. Daily Cash Flow Calculation

```java
public class DailyCashFlowCalculator {
    
    public DailyCashFlow calculateDailyCashFlow(
            SwapContract contract,
            LocalDate calculationDate,
            MarketDataSnapshot marketData,
            PositionSnapshot position) {
        
        DailyCashFlow cashFlow = new DailyCashFlow();
        
        // Calculate equity leg cash flows
        cashFlow.setEquityLeg(calculateEquityLegCashFlow(
            contract.getEquityLeg(), calculationDate, marketData, position));
        
        // Calculate interest leg cash flows
        cashFlow.setInterestLeg(calculateInterestLegCashFlow(
            contract.getInterestLeg(), calculationDate, marketData, position));
        
        // Calculate total cash flow
        cashFlow.setTotalCashFlow(
            cashFlow.getEquityLeg().getTotalAmount() + 
            cashFlow.getInterestLeg().getTotalAmount());
        
        return cashFlow;
    }
    
    private EquityLegCashFlow calculateEquityLegCashFlow(
            EquityLeg equityLeg,
            LocalDate calculationDate,
            MarketDataSnapshot marketData,
            PositionSnapshot position) {
        
        EquityLegCashFlow equityFlow = new EquityLegCashFlow();
        
        // Calculate P&L
        double currentPrice = marketData.getPrice(equityLeg.getUnderlying(), calculationDate);
        double costPrice = position.getCostPrice();
        double quantity = position.getQuantity();
        
        double unrealizedPnl = (currentPrice - costPrice) * quantity;
        equityFlow.setUnrealizedPnl(unrealizedPnl);
        
        // Calculate dividends
        List<Dividend> dividends = marketData.getDividends(
            equityLeg.getUnderlying(), calculationDate);
        
        double dividendAmount = dividends.stream()
            .mapToDouble(Dividend::getAmount)
            .sum() * quantity;
        
        equityFlow.setDividendAmount(dividendAmount);
        
        return equityFlow;
    }
}
```

### 2. Interest Accrual Calculation

```java
public class InterestAccrualCalculator {
    
    public InterestLegCashFlow calculateInterestAccrual(
            InterestLeg interestLeg,
            LocalDate calculationDate,
            MarketDataSnapshot marketData) {
        
        InterestLegCashFlow interestFlow = new InterestLegCashFlow();
        
        // Get applicable interest rate
        double rate = marketData.getRate(interestLeg.getIndex(), calculationDate);
        double spread = interestLeg.getSpread();
        double totalRate = rate + spread;
        
        // Calculate days since last reset
        LocalDate lastResetDate = getLastResetDate(interestLeg, calculationDate);
        int daysAccrued = (int) ChronoUnit.DAYS.between(lastResetDate, calculationDate);
        
        // Calculate accrued interest
        double notionalAmount = interestLeg.getNotionalAmount();
        double dayCountFraction = calculateDayCountFraction(
            lastResetDate, calculationDate, interestLeg.getDayCountConvention());
        
        double accruedInterest = notionalAmount * totalRate * dayCountFraction;
        
        interestFlow.setAccruedInterest(accruedInterest);
        interestFlow.setInterestRate(totalRate);
        interestFlow.setNotionalAmount(notionalAmount);
        
        return interestFlow;
    }
    
    private LocalDate getLastResetDate(InterestLeg interestLeg, LocalDate calculationDate) {
        // Find the most recent reset date before or equal to calculation date
        LocalDate effectiveDate = interestLeg.getEffectiveDate();
        Period resetPeriod = getResetPeriod(interestLeg.getResetFrequency());
        
        LocalDate lastReset = effectiveDate;
        while (lastReset.plus(resetPeriod).isBefore(calculationDate) || 
               lastReset.plus(resetPeriod).isEqual(calculationDate)) {
            lastReset = lastReset.plus(resetPeriod);
        }
        
        return lastReset;
    }
}
```

## Error Handling and Resilience

### 1. Circuit Breaker Pattern

```java
@Component
public class CalculationCircuitBreaker {
    
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;
    
    public CalculationResult executeWithResilience(CalculationTask task) {
        return circuitBreaker.runSupplier(() -> {
            try {
                return retryTemplate.execute(context -> {
                    return executeCalculation(task);
                });
            } catch (Exception e) {
                log.error("Calculation failed after retries", e);
                throw new CalculationException("Failed to calculate cash flows", e);
            }
        });
    }
    
    private CalculationResult executeCalculation(CalculationTask task) {
        // Validate inputs
        validateCalculationInputs(task);
        
        // Execute calculation
        return calculationEngine.calculate(task);
    }
}
```

### 2. Graceful Degradation

```java
@Component
public class GracefulDegradationService {
    
    public CalculationResult calculateWithDegradation(HistoricalDataContainer data, DateRange dateRange) {
        try {
            // Try full calculation first
            return calculationEngine.calculate(data, dateRange);
            
        } catch (OutOfMemoryError e) {
            log.warn("Memory limit exceeded, falling back to chunked processing");
            return calculateInChunks(data, dateRange);
            
        } catch (TimeoutException e) {
            log.warn("Calculation timeout, falling back to simplified algorithm");
            return calculateWithSimplifiedAlgorithm(data, dateRange);
        }
    }
    
    private CalculationResult calculateInChunks(HistoricalDataContainer data, DateRange dateRange) {
        List<DateRange> chunks = dateRangeOptimizer.optimizeDateRanges(dateRange);
        
        return chunks.parallelStream()
            .map(chunk -> calculationEngine.calculate(data, chunk))
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                this::mergeChunkResults
            ));
    }
}
```

## Monitoring and Observability

### 1. Performance Metrics

```java
@Component
public class CalculationMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer calculationTimer;
    private final Counter errorCounter;
    
    public void recordCalculationTime(String contractId, Duration duration) {
        calculationTimer.record(duration);
        meterRegistry.timer("calculation.duration", "contract", contractId)
                    .record(duration);
    }
    
    public void recordMemoryUsage(long bytesUsed) {
        meterRegistry.gauge("calculation.memory.used", bytesUsed);
    }
    
    public void recordError(String errorType) {
        errorCounter.increment();
        meterRegistry.counter("calculation.errors", "type", errorType)
                    .increment();
    }
}
```

### 2. Calculation Progress Tracking

```java
@Component
public class ProgressTracker {
    
    private final Map<String, CalculationProgress> progressMap;
    
    public void updateProgress(String requestId, int contractsProcessed, int totalContracts) {
        CalculationProgress progress = progressMap.get(requestId);
        if (progress != null) {
            progress.setContractsProcessed(contractsProcessed);
            progress.setTotalContracts(totalContracts);
            progress.setPercentage((double) contractsProcessed / totalContracts * 100);
        }
    }
    
    public CalculationProgress getProgress(String requestId) {
        return progressMap.get(requestId);
    }
}
```

## Implementation Checklist

### Phase 1: Core Engine
- [ ] Implement `HistoricalDataIngestionService`
- [ ] Create `TimeSeriesMap` data structure
- [ ] Build `HistoricalCalculationEngine`
- [ ] Implement `ContractCalculationTask`

### Phase 2: Performance Optimization
- [ ] Add `DateRangeOptimizer`
- [ ] Implement `MemoryManager` with object pooling
- [ ] Create `CalculationCache`
- [ ] Add parallel processing capabilities

### Phase 3: Calculation Algorithms
- [ ] Implement `DailyCashFlowCalculator`
- [ ] Build `InterestAccrualCalculator`
- [ ] Add P&L calculation logic
- [ ] Implement dividend processing

### Phase 4: Resilience
- [ ] Add circuit breaker pattern
- [ ] Implement graceful degradation
- [ ] Add retry mechanisms
- [ ] Create error handling strategies

### Phase 5: Monitoring
- [ ] Add performance metrics
- [ ] Implement progress tracking
- [ ] Create health checks
- [ ] Add logging and tracing

## Performance Targets

- **5-Year Contract Calculation**: <5 minutes
- **Memory Usage**: <4GB for largest contracts
- **Throughput**: 100+ contracts per minute
- **Error Rate**: <0.1%
- **Cache Hit Rate**: >80% for repeated calculations

## Testing Strategy

### 1. Unit Tests
- Test individual calculation components
- Verify mathematical accuracy
- Test edge cases and error conditions

### 2. Integration Tests
- Test end-to-end calculation flows
- Verify data consistency across components
- Test performance under load

### 3. Performance Tests
- Load test with 5-year contracts
- Memory leak detection
- Scalability testing

### 4. Regression Tests
- Compare results with legacy system
- Verify calculation reproducibility
- Test backward compatibility
