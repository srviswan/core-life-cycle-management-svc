# Cash Flow Management Service - Test Harness Specification

## Overview

This document provides detailed specifications for the test harness, including sample test data, test scenarios, and implementation guidelines to validate the Cash Flow Management Service against the original requirements.

## Test Harness Architecture

### Core Components

```yaml
Test Harness Components:
  Test Data Generator:
    - Contract Data Generator
    - Position Data Generator
    - Market Data Generator
    - Trade Event Generator
    - Corporate Action Generator
  
  Test Scenario Engine:
    - Unit Test Scenarios
    - Integration Test Scenarios
    - Performance Test Scenarios
    - End-to-End Test Scenarios
  
  Test Execution Engine:
    - Test Runner
    - Result Collector
    - Performance Monitor
    - Report Generator
  
  Validation Engine:
    - Calculation Validator
    - Data Integrity Validator
    - Performance Validator
    - SLA Compliance Validator
```

## Sample Test Data

### 1. Contract Test Data

#### Single Stock Swap Contract
```json
{
  "contractId": "SWAP_IBM_001",
  "underlying": "IBM",
  "notionalAmount": 400000,
  "currency": "USD",
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
```

#### Basket Swap Contract
```json
{
  "contractId": "BASKET_SP500_001",
  "contractType": "BASKET_SWAP",
  "tradeDate": "2019-01-01",
  "effectiveDate": "2019-01-01",
  "maturityDate": "2024-01-01",
  "notionalAmount": 1000000,
  "currency": "USD",
  "counterparties": {
    "buyer": "CLIENT_DEF",
    "seller": "BANK_XYZ"
  },
  "equityLeg": {
    "basketType": "S&P_500",
    "constituents": [
      {
        "underlying": "AAPL",
        "weight": 0.06,
        "quantity": 600
      },
      {
        "underlying": "MSFT",
        "weight": 0.05,
        "quantity": 500
      },
      {
        "underlying": "GOOGL",
        "weight": 0.04,
        "quantity": 400
      }
    ],
    "dividendTreatment": "PAY",
    "corporateActionHandling": "AUTOMATIC",
    "currency": "USD"
  },
  "interestLeg": {
    "rateType": "FLOATING",
    "index": "LIBOR_3M",
    "spread": 0.0030,
    "resetFrequency": "QUARTERLY",
    "dayCountConvention": "ACT_360",
    "currency": "USD",
    "notionalAmount": 1000000
  }
}
```

### 2. Position Test Data

#### Normal Position (1-1000 lots)
```json
{
  "positionId": "POS_IBM_001",
  "contractId": "SWAP_IBM_001",
  "underlying": "IBM",
  "totalQuantity": 1000,
  "currency": "USD",
  "status": "ACTIVE",
  "lots": [
    {
      "lotId": "LOT_001",
      "quantity": 1000,
      "costPrice": 120.00,
      "costDate": "2019-01-01",
      "lotType": "NEW_LOT",
      "status": "ACTIVE",
      "unwindingMethod": "LIFO"
    }
  ]
}
```

#### Complex Position (10K-65K lots)
```json
{
  "positionId": "POS_IBM_COMPLEX_001",
  "contractId": "SWAP_IBM_002",
  "underlying": "IBM",
  "totalQuantity": 65000,
  "currency": "USD",
  "status": "ACTIVE",
  "lots": [
    {
      "lotId": "LOT_001",
      "quantity": 10000,
      "costPrice": 120.00,
      "costDate": "2019-01-01",
      "lotType": "NEW_LOT",
      "status": "ACTIVE",
      "unwindingMethod": "LIFO"
    },
    {
      "lotId": "LOT_002",
      "quantity": 15000,
      "costPrice": 125.00,
      "costDate": "2020-06-15",
      "lotType": "NEW_LOT",
      "status": "ACTIVE",
      "unwindingMethod": "LIFO"
    },
    {
      "lotId": "LOT_003",
      "quantity": 20000,
      "costPrice": 130.00,
      "costDate": "2021-03-20",
      "lotType": "NEW_LOT",
      "status": "ACTIVE",
      "unwindingMethod": "LIFO"
    },
    {
      "lotId": "LOT_004",
      "quantity": 20000,
      "costPrice": 135.00,
      "costDate": "2022-09-10",
      "lotType": "NEW_LOT",
      "status": "ACTIVE",
      "unwindingMethod": "LIFO"
    }
  ]
}
```

### 3. Market Data Test Sets

#### Historical Price Data (5 years)
```json
{
  "symbol": "IBM",
  "currency": "USD",
  "prices": [
    {"date": "2019-01-01", "price": 120.00, "changeType": "OPENING"},
    {"date": "2019-01-02", "price": 121.50, "changeType": "CLOSING"},
    {"date": "2019-01-03", "price": 122.00, "changeType": "CLOSING"},
    {"date": "2019-03-15", "price": 125.00, "changeType": "EX_DIVIDEND"},
    {"date": "2019-03-20", "price": 124.50, "changeType": "PAYMENT"},
    {"date": "2020-03-15", "price": 110.00, "changeType": "EX_DIVIDEND"},
    {"date": "2020-03-20", "price": 109.50, "changeType": "PAYMENT"},
    {"date": "2021-03-15", "price": 130.00, "changeType": "EX_DIVIDEND"},
    {"date": "2021-03-20", "price": 129.50, "changeType": "PAYMENT"},
    {"date": "2022-03-15", "price": 140.00, "changeType": "EX_DIVIDEND"},
    {"date": "2022-03-20", "price": 139.50, "changeType": "PAYMENT"},
    {"date": "2023-03-15", "price": 150.00, "changeType": "EX_DIVIDEND"},
    {"date": "2023-03-20", "price": 149.50, "changeType": "PAYMENT"},
    {"date": "2024-01-13", "price": 155.00, "changeType": "EX_DIVIDEND"},
    {"date": "2024-01-15", "price": 154.50, "changeType": "CLOSING"}
  ]
}
```

#### Interest Rate Data
```json
{
  "index": "LIBOR_3M",
  "currency": "USD",
  "rates": [
    {"date": "2019-01-01", "rate": 0.0250, "changeType": "RESET"},
    {"date": "2019-04-01", "rate": 0.0275, "changeType": "RESET"},
    {"date": "2019-07-01", "rate": 0.0300, "changeType": "RESET"},
    {"date": "2019-10-01", "rate": 0.0325, "changeType": "RESET"},
    {"date": "2020-01-01", "rate": 0.0150, "changeType": "RESET"},
    {"date": "2020-04-01", "rate": 0.0125, "changeType": "RESET"},
    {"date": "2020-07-01", "rate": 0.0100, "changeType": "RESET"},
    {"date": "2020-10-01", "rate": 0.0125, "changeType": "RESET"},
    {"date": "2021-01-01", "rate": 0.0150, "changeType": "RESET"},
    {"date": "2021-04-01", "rate": 0.0200, "changeType": "RESET"},
    {"date": "2021-07-01", "rate": 0.0250, "changeType": "RESET"},
    {"date": "2021-10-01", "rate": 0.0300, "changeType": "RESET"},
    {"date": "2022-01-01", "rate": 0.0350, "changeType": "RESET"},
    {"date": "2022-04-01", "rate": 0.0400, "changeType": "RESET"},
    {"date": "2022-07-01", "rate": 0.0450, "changeType": "RESET"},
    {"date": "2022-10-01", "rate": 0.0500, "changeType": "RESET"},
    {"date": "2023-01-01", "rate": 0.0525, "changeType": "RESET"},
    {"date": "2023-04-01", "rate": 0.0550, "changeType": "RESET"},
    {"date": "2023-07-01", "rate": 0.0575, "changeType": "RESET"},
    {"date": "2023-10-01", "rate": 0.0600, "changeType": "RESET"},
    {"date": "2024-01-01", "rate": 0.0525, "changeType": "RESET"},
    {"date": "2024-01-15", "rate": 0.0530, "changeType": "RESET"}
  ]
}
```

#### Dividend Data
```json
{
  "symbol": "IBM",
  "currency": "USD",
  "dividends": [
    {
      "exDate": "2019-03-15",
      "recordDate": "2019-03-18",
      "paymentDate": "2019-03-20",
      "amount": 1.20,
      "withholdingTax": 0.12,
      "netAmount": 1.08,
      "dividendType": "REGULAR"
    },
    {
      "exDate": "2019-06-14",
      "recordDate": "2019-06-17",
      "paymentDate": "2019-06-20",
      "amount": 1.20,
      "withholdingTax": 0.12,
      "netAmount": 1.08,
      "dividendType": "REGULAR"
    },
    {
      "exDate": "2019-09-13",
      "recordDate": "2019-09-16",
      "paymentDate": "2019-09-20",
      "amount": 1.20,
      "withholdingTax": 0.12,
      "netAmount": 1.08,
      "dividendType": "REGULAR"
    },
    {
      "exDate": "2019-12-13",
      "recordDate": "2019-12-16",
      "paymentDate": "2019-12-20",
      "amount": 1.20,
      "withholdingTax": 0.12,
      "netAmount": 1.08,
      "dividendType": "REGULAR"
    },
    {
      "exDate": "2024-01-13",
      "recordDate": "2024-01-16",
      "paymentDate": "2024-01-20",
      "amount": 1.50,
      "withholdingTax": 0.15,
      "netAmount": 1.35,
      "dividendType": "REGULAR"
    }
  ]
}
```

## Test Scenarios

### 1. Unit Test Scenarios

#### Interest Calculation Tests
```java
@Test
void testInterestAccrualCalculation() {
    // Given: Contract with floating rate interest
    Contract contract = createFloatingRateContract();
    
    // When: Calculate interest for 3 months
    InterestCalculation result = interestCalculator.calculate(
        contract, 
        LocalDate.of(2019, 1, 1), 
        LocalDate.of(2019, 4, 1)
    );
    
    // Then: Verify interest calculation
    assertEquals(687.50, result.getAccruedInterest(), 0.01);
    assertEquals(0.0275, result.getInterestRate(), 0.0001);
    assertEquals(1000000, result.getNotionalAmount());
}

@Test
void testRateResetHandling() {
    // Given: Contract with quarterly rate resets
    Contract contract = createQuarterlyResetContract();
    
    // When: Calculate interest across rate reset
    InterestCalculation result = interestCalculator.calculate(
        contract,
        LocalDate.of(2019, 1, 1),
        LocalDate.of(2019, 7, 1)
    );
    
    // Then: Verify rate reset handling
    assertEquals(2, result.getRateResets().size());
    assertEquals(0.0250, result.getRateResets().get(0).getRate());
    assertEquals(0.0275, result.getRateResets().get(1).getRate());
}
```

#### Equity Calculation Tests
```java
@Test
void testP&lCalculation() {
    // Given: Position with multiple lots
    Position position = createMultiLotPosition();
    MarketData marketData = createMarketData();
    
    // When: Calculate P&L
    PnLCalculation result = equityCalculator.calculatePnL(position, marketData);
    
    // Then: Verify P&L calculation
    assertEquals(35000.00, result.getUnrealizedPnL(), 0.01);
    assertEquals(40000.00, result.getRealizedPnL(), 0.01);
    assertEquals(75000.00, result.getTotalPnL(), 0.01);
}

@Test
void testDividendHandling() {
    // Given: Position with dividend event
    Position position = createDividendPosition();
    DividendEvent dividend = createDividendEvent();
    
    // When: Calculate dividend cash flow
    DividendCashFlow result = equityCalculator.calculateDividend(position, dividend);
    
    // Then: Verify dividend calculation
    assertEquals(1200.00, result.getGrossDividend(), 0.01);
    assertEquals(120.00, result.getWithholdingTax(), 0.01);
    assertEquals(1080.00, result.getNetDividend(), 0.01);
}
```

#### Natural Key Tests
```java
@Test
void testCalculationRequestNaturalKey() {
    // Given: Calculation request with natural key components
    CalculationRequest request = createCalculationRequest();
    
    // When: Create calculation request
    CalculationRequest result = calculationService.createRequest(request);
    
    // Then: Verify natural key uniqueness
    assertNotNull(result.getRequestId());
    assertEquals("SWAP_IBM_001", result.getContractId());
    assertEquals(LocalDate.of(2019, 1, 1), result.getFromDate());
    assertEquals(LocalDate.of(2024, 1, 15), result.getToDate());
    assertEquals("HISTORICAL_RECALCULATION", result.getCalculationType());
}

@Test
void testSettlementNaturalKey() {
    // Given: Settlement instruction with natural key components
    SettlementInstruction settlement = createSettlementInstruction();
    
    // When: Create settlement instruction
    SettlementInstruction result = settlementService.createSettlement(settlement);
    
    // Then: Verify natural key uniqueness
    assertNotNull(result.getSettlementId());
    assertEquals("SWAP_IBM_001", result.getContractId());
    assertEquals("CF_001", result.getCashFlowId());
    assertEquals(LocalDate.of(2024, 1, 16), result.getSettlementDate());
    assertEquals("INTEREST", result.getSettlementType());
}
```

### 2. Integration Test Scenarios

#### API Endpoint Tests
```java
@Test
void testCashFlowCalculationEndpoint() {
    // Given: Complete cash flow request
    CashFlowRequest request = createCompleteCashFlowRequest();
    
    // When: Call calculation endpoint
    CashFlowResponse response = cashFlowController.calculate(request);
    
    // Then: Verify response
    assertEquals("CF_REQ_001", response.getRequestId());
    assertEquals("COMPLETED", response.getStatus());
    assertNotNull(response.getCalculationType());
    assertEquals(15, response.getCashFlows().size());
    assertEquals(76130.00, response.getSummary().getTotalCashFlows(), 0.01);
}

@Test
void testSettlementEndpoints() {
    // Given: Settlement creation request
    SettlementCreateRequest request = createSettlementRequest();
    
    // When: Create settlement
    SettlementInstruction settlement = settlementController.createSettlement(request);
    
    // Then: Verify settlement creation
    assertNotNull(settlement.getSettlementId());
    assertEquals("PENDING", settlement.getStatus());
    
    // When: Update settlement status
    SettlementStatusUpdate update = createStatusUpdate("COMPLETED");
    SettlementInstruction updated = settlementController.updateStatus(
        settlement.getSettlementId(), update
    );
    
    // Then: Verify status update
    assertEquals("COMPLETED", updated.getStatus());
}
```

### 3. Performance Test Scenarios

#### Load Test Scenarios
```java
@Test
void testDailyLoadSimulation() {
    // Given: 1M position updates over 24 hours
    LoadTestConfig config = LoadTestConfig.builder()
        .duration(Duration.ofHours(24))
        .totalRequests(1000000)
        .peakHour(16) // 4PM
        .peakMultiplier(4.0)
        .build();
    
    // When: Execute load test
    LoadTestResult result = loadTestRunner.execute(config);
    
    // Then: Verify performance metrics
    assertTrue(result.getSuccessRate() >= 0.9999);
    assertTrue(result.getP95ResponseTime() <= Duration.ofMillis(500));
    assertTrue(result.getP99ResponseTime() <= Duration.ofMillis(100));
}

@Test
void testPeakLoadSimulation() {
    // Given: 4M position updates during 4PM peak
    LoadTestConfig config = LoadTestConfig.builder()
        .duration(Duration.ofHours(2))
        .totalRequests(4000000)
        .peakPattern(PeakPattern.EXPONENTIAL)
        .build();
    
    // When: Execute peak load test
    LoadTestResult result = loadTestRunner.execute(config);
    
    // Then: Verify peak performance
    assertTrue(result.getSuccessRate() >= 0.999);
    assertTrue(result.getP95ResponseTime() <= Duration.ofSeconds(1));
    assertTrue(result.getP99ResponseTime() <= Duration.ofSeconds(2));
}
```

### 4. End-to-End Test Scenarios

#### Complete Workflow Tests
```java
@Test
void testTradeCaptureToSettlementWorkflow() {
    // Given: Trade capture event
    TradeEvent tradeEvent = createTradeEvent();
    
    // When: Process trade through complete workflow
    WorkflowResult result = workflowEngine.process(tradeEvent);
    
    // Then: Verify complete workflow
    assertNotNull(result.getCashFlow());
    assertNotNull(result.getSettlement());
    assertEquals("COMPLETED", result.getStatus());
    assertTrue(result.getProcessingTime() <= Duration.ofMinutes(15));
}

@Test
void testHistoricalRecalculationWorkflow() {
    // Given: 5-year contract for historical recalculation
    HistoricalRecalculationRequest request = createHistoricalRequest();
    
    // When: Execute historical recalculation
    HistoricalRecalculationResult result = historicalService.recalculate(request);
    
    // Then: Verify historical recalculation
    assertEquals("COMPLETED", result.getStatus());
    assertTrue(result.getProcessingTime() <= Duration.ofMinutes(5));
    assertEquals(1825, result.getCashFlows().size()); // 5 years * 365 days
    assertNotNull(result.getAuditTrail());
}
```

## Test Data Generation

### Synthetic Data Generator
```java
@Component
public class TestDataGenerator {
    
    public List<Contract> generateContracts(int count, ContractType type) {
        List<Contract> contracts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            contracts.add(createContract(i, type));
        }
        return contracts;
    }
    
    public List<Position> generatePositions(int count, PositionComplexity complexity) {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            positions.add(createPosition(i, complexity));
        }
        return positions;
    }
    
    public MarketData generateMarketData(LocalDate fromDate, LocalDate toDate) {
        return MarketData.builder()
            .securities(generateSecurityData(fromDate, toDate))
            .rates(generateRateData(fromDate, toDate))
            .dividends(generateDividendData(fromDate, toDate))
            .build();
    }
    
    private Contract createContract(int index, ContractType type) {
        return Contract.builder()
            .contractId("SWAP_" + type + "_" + String.format("%03d", index))
            .contractType(type)
            .tradeDate(LocalDate.of(2019, 1, 1))
            .effectiveDate(LocalDate.of(2019, 1, 1))
            .maturityDate(LocalDate.of(2024, 1, 1))
            .notionalAmount(400000 + (index * 100000))
            .currency("USD")
            .equityLeg(createEquityLeg(type))
            .interestLeg(createInterestLeg())
            .build();
    }
}
```

## Test Execution Framework

### Test Runner Configuration
```yaml
TestRunner:
  Unit Tests:
    - Calculation Engine Tests
    - Natural Key Tests
    - Data Model Tests
    - Duration: 5 minutes
    - Parallel Execution: true
  
  Integration Tests:
    - API Endpoint Tests
    - Database Integration Tests
    - Duration: 15 minutes
    - Parallel Execution: false
  
  Performance Tests:
    - Load Tests
    - Stress Tests
    - Duration: 2 hours
    - Parallel Execution: true
    - Resource Monitoring: true
  
  End-to-End Tests:
    - Complete Workflow Tests
    - Duration: 30 minutes
    - Parallel Execution: false
    - Environment: Full Stack
```

### Test Result Validation
```java
@Component
public class TestResultValidator {
    
    public ValidationResult validateCalculationAccuracy(CashFlowResponse response) {
        // Validate calculation accuracy
        // Compare with expected results
        // Check for calculation errors
        return ValidationResult.builder()
            .accuracy(calculateAccuracy(response))
            .errors(identifyErrors(response))
            .warnings(identifyWarnings(response))
            .build();
    }
    
    public ValidationResult validatePerformanceMetrics(LoadTestResult result) {
        // Validate performance metrics
        // Check SLA compliance
        // Identify performance bottlenecks
        return ValidationResult.builder()
            .slaCompliance(checkSLACompliance(result))
            .performanceIssues(identifyPerformanceIssues(result))
            .recommendations(generateRecommendations(result))
            .build();
    }
    
    public ValidationResult validateDataIntegrity(TestResult result) {
        // Validate data integrity
        // Check natural key constraints
        // Verify audit trail completeness
        return ValidationResult.builder()
            .integrityScore(calculateIntegrityScore(result))
            .constraintViolations(identifyConstraintViolations(result))
            .auditCompleteness(checkAuditCompleteness(result))
            .build();
    }
}
```

This comprehensive test harness specification provides the foundation for validating the Cash Flow Management Service against all original requirements, ensuring high quality, performance, and reliability.
