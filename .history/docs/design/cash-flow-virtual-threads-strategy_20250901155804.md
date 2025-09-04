# Virtual Threads Strategy for Cash Flow Management Service

## Overview

Java 21's virtual threads provide lightweight, scalable concurrency that can significantly improve performance for I/O-bound operations. This document outlines where virtual threads should and shouldn't be used in the Cash Flow Management Service.

## Virtual Threads Benefits

### 1. **I/O-Bound Operations**
- **Database queries** - Waiting for SQL results
- **HTTP calls** - Market data API calls
- **File I/O** - Reading/writing archival data
- **Network operations** - Service-to-service communication

### 2. **Scalability**
- **Millions of threads** - Can handle thousands of concurrent operations
- **Low memory overhead** - ~1KB per virtual thread vs ~1MB per platform thread
- **Fast creation/destruction** - No OS thread overhead

### 3. **Simplified Code**
- **Synchronous style** - Write blocking code that doesn't block threads
- **Better error handling** - Exceptions propagate naturally
- **Easier debugging** - Stack traces are clearer

## Recommended Use Cases

### 1. **Market Data Service** ✅ **HIGH PRIORITY**
```java
@Service
public class MarketDataService {
    
    public CompletableFuture<MarketData> loadMarketDataAsync(CashFlowRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // Parallel market data loading
                var priceFuture = executor.submit(() -> 
                    externalClient.getPrice(request.getUnderlying()));
                var rateFuture = executor.submit(() -> 
                    externalClient.getRate(request.getIndex()));
                var dividendFuture = executor.submit(() -> 
                    externalClient.getDividends(request.getUnderlying()));
                
                return MarketData.builder()
                    .price(priceFuture.get())
                    .rate(rateFuture.get())
                    .dividends(dividendFuture.get())
                    .build();
            }
        });
    }
}
```

### 2. **Database Operations** ✅ **HIGH PRIORITY**
```java
@Repository
public class CashFlowRepository {
    
    public List<CashFlow> getCashFlowsByContract(String contractId, 
                                                LocalDate fromDate, 
                                                LocalDate toDate) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Parallel database queries
            var cashFlowsFuture = executor.submit(() -> 
                jdbcTemplate.query("SELECT * FROM cash_flows WHERE contract_id = ?", 
                                 contractId));
            var schedulesFuture = executor.submit(() -> 
                jdbcTemplate.query("SELECT * FROM payment_schedules WHERE contract_id = ?", 
                                 contractId));
            
            return processResults(cashFlowsFuture.get(), schedulesFuture.get());
        }
    }
}
```

### 3. **Historical Recalculation Engine** ✅ **HIGH PRIORITY**
```java
@Component
public class HistoricalCalculationEngine {
    
    public CashFlowResponse calculateHistorical(CashFlowRequest request) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Process contracts in parallel
            var contractFutures = request.getContracts().stream()
                .map(contract -> executor.submit(() -> 
                    calculateContractCashFlows(contract, request)))
                .collect(Collectors.toList());
            
            // Collect results
            var contractResults = contractFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            return buildResponse(contractResults);
        }
    }
}
```

### 4. **Archival Operations** ✅ **MEDIUM PRIORITY**
```java
@Component
public class ArchivalService {
    
    public void archiveHistoricalData(int retentionMonths) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Parallel archival operations
            var cashFlowsFuture = executor.submit(() -> 
                archiveCashFlows(retentionMonths));
            var lotsFuture = executor.submit(() -> 
                archiveLots(retentionMonths));
            var positionsFuture = executor.submit(() -> 
                archivePositions(retentionMonths));
            
            // Wait for all operations to complete
            CompletableFuture.allOf(cashFlowsFuture, lotsFuture, positionsFuture).join();
        }
    }
}
```

### 5. **Settlement Processing** ✅ **MEDIUM PRIORITY**
```java
@Component
public class SettlementService {
    
    public void processSettlements(List<SettlementInstruction> settlements) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Process settlements in parallel
            var futures = settlements.stream()
                .map(settlement -> executor.submit(() -> 
                    processSettlement(settlement)))
                .collect(Collectors.toList());
            
            // Handle results
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("Settlement processing failed", e);
                }
            });
        }
    }
}
```

## NOT Recommended Use Cases

### 1. **CPU-Intensive Calculations** ❌ **AVOID**
```java
// DON'T use virtual threads for CPU-intensive work
public class PnLCalculator {
    
    // ❌ BAD: CPU-intensive calculation in virtual thread
    public double calculatePnL(List<Trade> trades) {
        return Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            // CPU-intensive calculations
            return trades.stream()
                .mapToDouble(this::calculateTradePnL)
                .sum();
        }).join();
    }
    
    // ✅ GOOD: Use platform threads for CPU work
    public double calculatePnL(List<Trade> trades) {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            .submit(() -> {
                return trades.stream()
                    .mapToDouble(this::calculateTradePnL)
                    .sum();
            }).join();
    }
}
```

### 2. **Real-Time Processing** ❌ **AVOID**
```java
// DON'T use virtual threads for real-time calculations
public class RealTimeCalculationEngine {
    
    // ❌ BAD: Virtual threads add overhead for real-time
    public CashFlowResponse calculateRealTime(CashFlowRequest request) {
        return Executors.newVirtualThreadPerTaskExecutor()
            .submit(() -> calculateCashFlows(request))
            .join(); // This adds unnecessary overhead
    }
    
    // ✅ GOOD: Direct calculation for real-time
    public CashFlowResponse calculateRealTime(CashFlowRequest request) {
        return calculateCashFlows(request); // Direct execution
    }
}
```

### 3. **Synchronous Operations** ❌ **AVOID**
```java
// DON'T use virtual threads for simple synchronous operations
public class SimpleDataService {
    
    // ❌ BAD: Unnecessary overhead
    public String getContractStatus(String contractId) {
        return Executors.newVirtualThreadPerTaskExecutor()
            .submit(() -> jdbcTemplate.queryForObject(
                "SELECT status FROM contracts WHERE contract_id = ?", 
                String.class, contractId))
            .join();
    }
    
    // ✅ GOOD: Direct execution
    public String getContractStatus(String contractId) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM contracts WHERE contract_id = ?", 
            String.class, contractId);
    }
}
```

## Configuration Strategy

### 1. **Thread Pool Configuration**
```java
@Configuration
public class VirtualThreadConfig {
    
    @Bean
    public ExecutorService virtualThreadExecutor() {
        // For I/O-bound operations
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Bean
    public ExecutorService cpuThreadExecutor() {
        // For CPU-intensive operations
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    }
    
    @Bean
    public ExecutorService mixedThreadExecutor() {
        // For mixed workloads
        return Executors.newWorkStealingPool();
    }
}
```

### 2. **Spring Boot Configuration**
```yaml
# application.yml
spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
      thread-name-prefix: "cashflow-"
```

### 3. **Monitoring and Metrics**
```java
@Component
public class VirtualThreadMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordVirtualThreadUsage(String operation, long duration) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("virtual.thread.operation")
            .tag("operation", operation)
            .register(meterRegistry));
    }
}
```

## Performance Considerations

### 1. **Memory Usage**
- **Virtual threads**: ~1KB per thread
- **Platform threads**: ~1MB per thread
- **Benefit**: 1000x memory efficiency

### 2. **Context Switching**
- **Virtual threads**: Very fast (user-space)
- **Platform threads**: Slower (kernel-space)
- **Benefit**: Better throughput for I/O operations

### 3. **Thread Creation**
- **Virtual threads**: Instant creation
- **Platform threads**: Expensive creation
- **Benefit**: No thread pool management needed

## Migration Strategy

### Phase 1: Market Data Service
```java
// Start with external API calls
public class MarketDataClient {
    
    public MarketData getMarketData(String symbol) {
        return Executors.newVirtualThreadPerTaskExecutor()
            .submit(() -> {
                // HTTP calls to external APIs
                return externalApiClient.getData(symbol);
            }).join();
    }
}
```

### Phase 2: Database Operations
```java
// Add to database operations
public class CashFlowRepository {
    
    public List<CashFlow> getCashFlows(String contractId) {
        return Executors.newVirtualThreadPerTaskExecutor()
            .submit(() -> {
                // Database queries
                return jdbcTemplate.query("SELECT * FROM cash_flows", ...);
            }).join();
    }
}
```

### Phase 3: Historical Calculations
```java
// Parallel contract processing
public class HistoricalEngine {
    
    public CashFlowResponse calculate(List<Contract> contracts) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = contracts.stream()
                .map(contract -> executor.submit(() -> 
                    calculateContract(contract)))
                .collect(Collectors.toList());
            
            return buildResponse(futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
        }
    }
}
```

## Best Practices

### 1. **Use Virtual Threads for I/O**
- Database operations
- HTTP calls
- File I/O
- Network operations

### 2. **Use Platform Threads for CPU**
- Complex calculations
- Data processing
- Algorithm execution

### 3. **Monitor Performance**
- Track thread usage
- Measure response times
- Monitor memory usage

### 4. **Gradual Migration**
- Start with non-critical paths
- Test thoroughly
- Monitor for issues

## Conclusion

Virtual threads are **highly recommended** for the Cash Flow Management Service, particularly for:

1. **Market data API calls** - External HTTP requests
2. **Database operations** - I/O-bound queries
3. **Historical calculations** - Parallel contract processing
4. **Archival operations** - File I/O operations

**Avoid** using virtual threads for:
1. **Real-time calculations** - Direct execution is faster
2. **CPU-intensive work** - Platform threads are better
3. **Simple synchronous operations** - Unnecessary overhead

The key is to **use the right tool for the job** - virtual threads for I/O-bound operations and platform threads for CPU-bound operations.
