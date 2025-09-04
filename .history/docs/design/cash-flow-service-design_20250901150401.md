# Cash Flow Management Service Design

## Overview

The Cash Flow Management Service is a self-contained, high-performance service responsible for calculating cash flows for synthetic swap contracts. It supports three calculation modes: real-time processing, incremental updates, and historical recalculation.

## Service Architecture

### Core Principles
1. **Self-Contained**: Receives all necessary data in requests
2. **Deterministic**: Same inputs always produce same outputs
3. **High Performance**: <100ms for real-time, <5min for historical
4. **Hybrid Data Strategy**: Market data endpoints + self-contained data
5. **Schedule-Driven**: Payment schedules determine when to calculate

### Service Boundaries
- **Input**: Contract details, positions, lots, market data, payment schedules
- **Processing**: Cash flow calculations based on schedules and market data
- **Output**: Cash flow events, settlement instructions, P&L reports
- **No External Dependencies**: During calculation processing

## Data Models

### 1. Input Request Model

```json
{
  "cashFlowRequest": {
    "requestId": "CF_REQ_001",
    "calculationType": "HISTORICAL_RECALCULATION",
    "dateRange": {
      "fromDate": "2019-01-01",
      "toDate": "2024-01-15",
      "calculationFrequency": "DAILY"
    },
    "contracts": [
      {
        "contractId": "SWAP_IBM_001",
        "basketContractId": "BASKET_001",
        "underlying": "IBM",
        "tradeDate": "2019-01-01",
        "effectiveDate": "2019-01-01",
        "maturityDate": "2024-01-01",
        "notionalAmount": 400000,
        "currency": "USD",
        "weight": 0.4,
        "counterparties": {
          "buyer": "CLIENT_ABC",
          "seller": "BANK_XYZ"
        },
        "equityLeg": {
          "underlying": "IBM",
          "quantity": 1000,
          "dividendTreatment": "REINVEST",
          "corporateActionHandling": "AUTOMATIC",
          "currency": "USD"
        },
        "interestLeg": {
          "rateType": "FLOATING",
          "index": "LIBOR_3M",
          "spread": 0.0025,
          "resetFrequency": "QUARTERLY",
          "dayCountConvention": "ACT_360",
          "currency": "USD",
          "notionalAmount": 400000
        }
      }
    ],
    "positions": [
      {
        "positionId": "POS_IBM_001",
        "contractId": "SWAP_IBM_001",
        "underlying": "IBM",
        "totalQuantity": 1000,
        "currency": "USD",
        "status": "ACTIVE"
      }
    ],
    "lots": [
      {
        "lotId": "LOT_001",
        "contractId": "SWAP_IBM_001",
        "positionId": "POS_IBM_001",
        "underlying": "IBM",
        "quantity": 1000,
        "costPrice": 120.00,
        "costDate": "2019-01-01",
        "lotType": "NEW_LOT",
        "status": "ACTIVE",
        "unwindingMethod": "LIFO"
      }
    ],
    "paymentSchedules": [
      {
        "scheduleId": "SCH_001",
        "contractId": "SWAP_IBM_001",
        "scheduleType": "INTEREST_PAYMENT",
        "scheduledDate": "2019-04-01",
        "frequency": "QUARTERLY",
        "leg": "INTEREST",
        "notionalAmount": 400000,
        "version": 1,
        "effectiveFrom": "2019-01-01",
        "effectiveTo": "2019-04-01"
      },
      {
        "scheduleId": "SCH_002",
        "contractId": "SWAP_IBM_001",
        "scheduleType": "DIVIDEND_PAYMENT",
        "scheduledDate": "2019-03-20",
        "frequency": "AS_DECLARED",
        "leg": "EQUITY",
        "version": 1,
        "effectiveFrom": "2019-01-01",
        "effectiveTo": "2019-03-20"
      }
    ],
    "marketDataStrategy": {
      "mode": "HYBRID",
      "realTimeEndpoints": {
        "enabled": true,
        "baseUrl": "https://market-data-service/api/v1",
        "timeoutMs": 5000
      },
      "localCache": {
        "enabled": true,
        "ttlHours": 24,
        "maxSizeMB": 1000
      },
      "compression": {
        "enabled": true,
        "algorithm": "GZIP",
        "deltaEncoding": true
      }
    },
    "marketData": {
      "mode": "SELF_CONTAINED",
      "compressed": true,
      "data": {
        "securities": [
          {
            "symbol": "IBM",
            "basePrice": 120.00,
            "baseDate": "2019-01-01",
            "changes": [
              {"date": "2019-01-02", "change": 1.20},
              {"date": "2019-01-03", "change": -0.50},
              {"date": "2024-01-15", "change": 35.00}
            ]
          }
        ],
        "rates": [
          {
            "index": "LIBOR_3M",
            "baseRate": 0.0250,
            "baseDate": "2019-01-01",
            "changes": [
              {"date": "2019-04-01", "change": 0.0025},
              {"date": "2024-01-15", "change": 0.0275}
            ]
          }
        ],
        "dividends": [
          {
            "symbol": "IBM",
            "dividends": [
              {
                "exDate": "2019-03-15",
                "paymentDate": "2019-03-20",
                "amount": 1.20,
                "currency": "USD"
              },
              {
                "exDate": "2024-01-13",
                "paymentDate": "2024-01-20",
                "amount": 1.50,
                "currency": "USD"
              }
            ]
          }
        ]
      }
    }
  }
}
```

### 2. Output Response Model

```json
{
  "cashFlowResponse": {
    "requestId": "CF_REQ_001",
    "calculationDate": "2024-01-15",
    "dateRange": {
      "fromDate": "2019-01-01",
      "toDate": "2024-01-15"
    },
    "calculationType": "HISTORICAL_RECALCULATION",
    "summary": {
      "totalContracts": 1,
      "totalPositions": 1,
      "totalLots": 1,
      "totalCashFlows": 1825,
      "totalAmount": 76130.00,
      "currency": "USD"
    },
    "contractResults": [
      {
        "contractId": "SWAP_IBM_001",
        "basketContractId": "BASKET_001",
        "underlying": "IBM",
        "summary": {
          "totalCashFlows": 76130.00,
          "totalInterest": 1250.00,
          "totalDividends": 1080.00,
          "totalP&l": 75000.00
        },
        "positionResults": [
          {
            "positionId": "POS_IBM_001",
            "underlying": "IBM",
            "summary": {
              "totalCashFlows": 76130.00,
              "totalInterest": 1250.00,
              "totalDividends": 1080.00,
              "totalP&l": 75000.00
            },
            "lotResults": [
              {
                "lotId": "LOT_001",
                "summary": {
                  "totalCashFlows": 76130.00,
                  "totalInterest": 1250.00,
                  "totalDividends": 1080.00,
                  "totalP&l": 75000.00
                },
                "cashFlows": [
                  {
                    "date": "2019-01-01",
                    "cashFlowType": "INTEREST_ACCRUAL",
                    "equityLeg": {
                      "unrealizedP&l": 0.00,
                      "realizedP&l": 0.00,
                      "totalP&l": 0.00
                    },
                    "interestLeg": {
                      "accruedInterest": 0.00,
                      "interestRate": 0.0250,
                      "notionalAmount": 400000
                    },
                    "totalCashFlow": 0.00,
                    "currency": "USD"
                  },
                  {
                    "date": "2019-03-20",
                    "cashFlowType": "DIVIDEND",
                    "equityLeg": {
                      "dividendAmount": 1200.00,
                      "withholdingTax": 120.00,
                      "netDividend": 1080.00
                    },
                    "interestLeg": {
                      "accruedInterest": 0.00,
                      "interestRate": 0.0250,
                      "notionalAmount": 400000
                    },
                    "totalCashFlow": 1080.00,
                    "currency": "USD"
                  },
                  {
                    "date": "2024-01-15",
                    "cashFlowType": "LOT_CLOSURE",
                    "equityLeg": {
                      "unrealizedP&l": 35000.00,
                      "realizedP&l": 40000.00,
                      "totalP&l": 75000.00
                    },
                    "interestLeg": {
                      "accruedInterest": 50.00,
                      "interestRate": 0.0525,
                      "notionalAmount": 400000
                    },
                    "totalCashFlow": 75050.00,
                    "currency": "USD"
                  }
                ]
              }
            ]
          }
        ]
      }
    ],
    "settlementInstructions": [
      {
        "instructionId": "SI_001",
        "contractId": "SWAP_IBM_001",
        "lotId": "LOT_001",
        "cashFlowType": "LOT_CLOSURE",
        "amount": 75050.00,
        "currency": "USD",
        "counterparty": "CLIENT_ABC",
        "settlementDate": "2024-01-17",
        "settlementMethod": "CASH",
        "status": "PENDING"
      }
    ],
    "metadata": {
      "calculationVersion": "1.0",
      "calculationEngine": "UnifiedCashFlowEngine",
      "processingTimeMs": 180000,
      "memoryUsageMB": 2048,
      "cacheHitRate": 0.85,
      "dataSource": "HYBRID"
    }
  }
}
```

## API Design

### 1. REST Endpoints

```java
@RestController
@RequestMapping("/api/v1/cashflows")
public class CashFlowController {

    @PostMapping("/calculate")
    public ResponseEntity<CashFlowResponse> calculateCashFlows(
            @RequestBody CashFlowRequest request) {
        
        CashFlowResponse response = cashFlowService.calculate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate/real-time")
    public ResponseEntity<CashFlowResponse> calculateRealTime(
            @RequestBody CashFlowRequest request) {
        
        request.setCalculationType("REAL_TIME_PROCESSING");
        CashFlowResponse response = cashFlowService.calculate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate/historical")
    public ResponseEntity<CashFlowResponse> calculateHistorical(
            @RequestBody CashFlowRequest request) {
        
        request.setCalculationType("HISTORICAL_RECALCULATION");
        CashFlowResponse response = cashFlowService.calculate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{requestId}")
    public ResponseEntity<CalculationStatus> getCalculationStatus(
            @PathVariable String requestId) {
        
        CalculationStatus status = cashFlowService.getStatus(requestId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/cashflows/{contractId}")
    public ResponseEntity<List<CashFlow>> getCashFlowsByContract(
            @PathVariable String contractId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        
        List<CashFlow> cashFlows = cashFlowService.getCashFlows(contractId, fromDate, toDate);
        return ResponseEntity.ok(cashFlows);
    }

    @GetMapping("/settlements/pending")
    public ResponseEntity<List<SettlementInstruction>> getPendingSettlements() {
        
        List<SettlementInstruction> settlements = cashFlowService.getPendingSettlements();
        return ResponseEntity.ok(settlements);
    }
}
```

### 2. Event-Driven APIs

```java
@Component
public class CashFlowEventHandler {

    @EventListener
    public void handleScheduleModified(ScheduleModifiedEvent event) {
        cashFlowService.handleScheduleModification(event);
    }

    @EventListener
    public void handleContractCreated(ContractCreatedEvent event) {
        cashFlowService.handleContractCreation(event);
    }

    @EventListener
    public void handleMarketDataUpdated(MarketDataUpdatedEvent event) {
        cashFlowService.handleMarketDataUpdate(event);
    }
}
```

## Core Components

### 1. Unified Cash Flow Engine

```java
@Component
public class UnifiedCashFlowEngine {
    
    private final CalculationModeSelector modeSelector;
    private final RealTimeCalculationEngine realTimeEngine;
    private final HistoricalCalculationEngine historicalEngine;
    private final IncrementalCalculationEngine incrementalEngine;
    private final MarketDataService marketDataService;
    private final ScheduleService scheduleService;
    
    public CashFlowResponse calculate(CashFlowRequest request) {
        // 1. Determine calculation mode
        CalculationMode mode = modeSelector.selectMode(request);
        
        // 2. Load market data based on strategy
        MarketDataContainer marketData = marketDataService.loadMarketData(request);
        
        // 3. Process schedules
        List<PaymentSchedule> schedules = scheduleService.processSchedules(request);
        
        // 4. Execute calculation based on mode
        switch (mode) {
            case REAL_TIME_PROCESSING:
                return realTimeEngine.calculate(request, marketData, schedules);
            case HISTORICAL_RECALCULATION:
                return historicalEngine.calculate(request, marketData, schedules);
            case INCREMENTAL_UPDATE:
                return incrementalEngine.calculate(request, marketData, schedules);
            default:
                throw new UnsupportedCalculationModeException(mode);
        }
    }
}
```

### 2. Calculation Mode Selector

```java
@Component
public class CalculationModeSelector {
    
    public CalculationMode selectMode(CashFlowRequest request) {
        DateRange dateRange = request.getDateRange();
        int daysInRange = dateRange.getDays();
        
        if (daysInRange > 30) {
            return CalculationMode.HISTORICAL_RECALCULATION;
        } else if (daysInRange == 1) {
            return CalculationMode.REAL_TIME_PROCESSING;
        } else {
            return CalculationMode.INCREMENTAL_UPDATE;
        }
    }
}
```

### 3. Market Data Service

```java
@Component
public class MarketDataService {
    
    private final MarketDataCache localCache;
    private final MarketDataClient externalClient;
    
    public MarketDataContainer loadMarketData(CashFlowRequest request) {
        MarketDataStrategy strategy = request.getMarketDataStrategy();
        
        switch (strategy.getMode()) {
            case HYBRID:
                return loadHybridMarketData(request);
            case SELF_CONTAINED:
                return loadSelfContainedMarketData(request);
            case ENDPOINTS:
                return loadFromEndpoints(request);
            default:
                throw new UnsupportedMarketDataModeException(strategy.getMode());
        }
    }
    
    private MarketDataContainer loadHybridMarketData(CashFlowRequest request) {
        MarketDataContainer container = new MarketDataContainer();
        
        // 1. Try local cache first
        MarketData cached = localCache.get(request.getCacheKey());
        if (cached != null && cached.isValid()) {
            container.addAll(cached);
        }
        
        // 2. Fill gaps from endpoints
        MarketData missing = externalClient.getMissingData(request);
        container.addAll(missing);
        
        // 3. Fallback to self-contained if needed
        if (container.isEmpty()) {
            container = loadSelfContainedMarketData(request);
        }
        
        return container;
    }
}
```

### 4. Schedule Service

```java
@Component
public class ScheduleService {
    
    public List<PaymentSchedule> processSchedules(CashFlowRequest request) {
        List<PaymentSchedule> schedules = request.getPaymentSchedules();
        
        // 1. Filter schedules within date range
        schedules = filterSchedulesByDateRange(schedules, request.getDateRange());
        
        // 2. Group schedules by contract and lot
        Map<String, List<PaymentSchedule>> groupedSchedules = groupSchedules(schedules);
        
        // 3. Sort schedules by date
        schedules = sortSchedulesByDate(schedules);
        
        return schedules;
    }
    
    private List<PaymentSchedule> filterSchedulesByDateRange(
            List<PaymentSchedule> schedules, DateRange dateRange) {
        
        return schedules.stream()
            .filter(schedule -> 
                schedule.getScheduledDate().isAfter(dateRange.getFromDate().minusDays(1)) &&
                schedule.getScheduledDate().isBefore(dateRange.getToDate().plusDays(1)))
            .collect(Collectors.toList());
    }
}
```

## Calculation Algorithms

### 1. Daily Cash Flow Calculation

```java
public class DailyCashFlowCalculator {
    
    public DailyCashFlow calculateDailyCashFlow(
            SwapContract contract,
            PaymentSchedule schedule,
            MarketDataSnapshot marketData,
            LotSnapshot lot) {
        
        DailyCashFlow cashFlow = new DailyCashFlow();
        
        // Calculate based on schedule type
        switch (schedule.getScheduleType()) {
            case INTEREST_PAYMENT:
                cashFlow.setInterestLeg(calculateInterestPayment(contract, schedule, marketData));
                break;
            case DIVIDEND_PAYMENT:
                cashFlow.setEquityLeg(calculateDividendPayment(contract, schedule, marketData, lot));
                break;
            case LOT_CLOSURE:
                cashFlow.setEquityLeg(calculateLotClosure(contract, schedule, marketData, lot));
                break;
            default:
                cashFlow.setInterestLeg(calculateInterestAccrual(contract, schedule, marketData));
        }
        
        // Calculate total
        cashFlow.setTotalCashFlow(
            cashFlow.getEquityLeg().getTotalAmount() + 
            cashFlow.getInterestLeg().getTotalAmount());
        
        return cashFlow;
    }
    
    private InterestLegCashFlow calculateInterestPayment(
            SwapContract contract, 
            PaymentSchedule schedule, 
            MarketDataSnapshot marketData) {
        
        InterestLegCashFlow interestFlow = new InterestLegCashFlow();
        
        // Get interest rate
        double rate = marketData.getRate(contract.getInterestLeg().getIndex(), schedule.getScheduledDate());
        double spread = contract.getInterestLeg().getSpread();
        double totalRate = rate + spread;
        
        // Calculate interest amount
        double notionalAmount = schedule.getNotionalAmount();
        double dayCountFraction = calculateDayCountFraction(
            schedule.getEffectiveFrom(), 
            schedule.getScheduledDate(), 
            contract.getInterestLeg().getDayCountConvention());
        
        double interestAmount = notionalAmount * totalRate * dayCountFraction;
        
        interestFlow.setAccruedInterest(interestAmount);
        interestFlow.setInterestRate(totalRate);
        interestFlow.setNotionalAmount(notionalAmount);
        
        return interestFlow;
    }
}
```

### 2. P&L Calculation

```java
public class PnLCalculator {
    
    public EquityLegCashFlow calculatePnL(
            SwapContract contract,
            PaymentSchedule schedule,
            MarketDataSnapshot marketData,
            LotSnapshot lot) {
        
        EquityLegCashFlow equityFlow = new EquityLegCashFlow();
        
        // Get current price
        double currentPrice = marketData.getPrice(contract.getEquityLeg().getUnderlying(), 
                                                schedule.getScheduledDate());
        double costPrice = lot.getCostPrice();
        double quantity = lot.getQuantity();
        
        // Calculate P&L
        double unrealizedPnl = (currentPrice - costPrice) * quantity;
        double realizedPnl = 0.0; // Set based on lot closure logic
        
        equityFlow.setUnrealizedPnl(unrealizedPnl);
        equityFlow.setRealizedPnl(realizedPnl);
        equityFlow.setTotalPnl(unrealizedPnl + realizedPnl);
        
        return equityFlow;
    }
}
```

## Database Schema

### Core Tables

```sql
-- Cash flows table (partitioned by calculation date)
CREATE TABLE cash_flows (
    cash_flow_id VARCHAR(50) NOT NULL,
    request_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
    position_id VARCHAR(50) NOT NULL,
    lot_id VARCHAR(50) NOT NULL,
    schedule_id VARCHAR(50),
    calculation_date DATE NOT NULL,
    cash_flow_type VARCHAR(30) NOT NULL,
    equity_leg_amount DECIMAL(20,2),
    interest_leg_amount DECIMAL(20,2),
    total_amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    state VARCHAR(20) DEFAULT 'REALIZED_UNSETTLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cash_flow_id, calculation_date)
) PARTITION BY RANGE (calculation_date);

-- Calculation requests table
CREATE TABLE calculation_requests (
    request_id VARCHAR(50) PRIMARY KEY,
    calculation_type VARCHAR(30) NOT NULL,
    date_range_from DATE NOT NULL,
    date_range_to DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    total_contracts INTEGER,
    processed_contracts INTEGER DEFAULT 0,
    processing_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Settlement instructions table
CREATE TABLE settlement_instructions (
    instruction_id VARCHAR(50) PRIMARY KEY,
    cash_flow_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
    lot_id VARCHAR(50) NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    settlement_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Performance Requirements

### Throughput Targets
- **Real-Time Processing**: 10,000+ calculations per minute
- **Incremental Updates**: 1,000+ calculations per minute  
- **Historical Recalculation**: 100+ contracts per minute

### Latency Targets
- **Real-Time Processing**: <100ms per calculation
- **Incremental Updates**: <1 minute for 30-day range
- **Historical Recalculation**: <5 minutes for 5-year contracts

### Resource Usage
- **Memory**: <4GB for largest historical calculations
- **CPU**: <80% utilization under peak load
- **Storage**: <1TB for 7 years of data retention

## Error Handling

### Exception Types
```java
public enum CashFlowErrorType {
    INVALID_CONTRACT_DATA,
    MISSING_MARKET_DATA,
    SCHEDULE_CONFLICT,
    CALCULATION_ERROR,
    MEMORY_LIMIT_EXCEEDED,
    TIMEOUT_ERROR
}
```

### Retry Strategy
```java
@Component
public class RetryHandler {
    
    public CashFlowResponse executeWithRetry(CashFlowRequest request) {
        return retryTemplate.execute(context -> {
            try {
                return cashFlowEngine.calculate(request);
            } catch (MemoryLimitExceededException e) {
                // Fallback to chunked processing
                return processInChunks(request);
            } catch (TimeoutException e) {
                // Fallback to simplified algorithm
                return processWithSimplifiedAlgorithm(request);
            }
        });
    }
}
```

## Monitoring and Observability

### Key Metrics
- **Calculation Duration**: Processing time per request
- **Memory Usage**: Peak memory consumption
- **Cache Hit Rate**: Market data cache efficiency
- **Error Rate**: Failed calculations percentage
- **Throughput**: Calculations per second

### Health Checks
```java
@Component
public class HealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            databaseHealthCheck();
            
            // Check market data service
            marketDataHealthCheck();
            
            // Check memory usage
            memoryHealthCheck();
            
            return Health.up()
                .withDetail("status", "Cash Flow Service is healthy")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

This comprehensive design provides a robust, scalable, and performant Cash Flow Management Service that can handle all calculation modes while maintaining data integrity and regulatory compliance.
