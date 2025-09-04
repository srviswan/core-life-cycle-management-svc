# Conventional Implementation Strategy for Cash Flow Management Service

## Overview

This document outlines the conventional implementation approach for the Cash Flow Management Service using:
- **Spring Boot** with traditional MVC
- **Synchronous APIs** with blocking operations
- **Platform threads** with virtual threads for I/O
- **MS SQL Server** with temporal tables
- **Simple, maintainable code** patterns

## Architecture Overview

### **Service Stack**
```
┌─────────────────────────────────────────────────────────────┐
│                    Cash Flow Management Service              │
├─────────────────────────────────────────────────────────────┤
│  Spring Boot 3.x + Java 21                                  │
│  ├── Spring MVC (REST APIs)                                 │
│  ├── Spring Data JPA (Database Access)                      │
│  ├── Virtual Threads (I/O Operations)                       │
│  └── Platform Threads (CPU Work)                           │
├─────────────────────────────────────────────────────────────┤
│  MS SQL Server + Temporal Tables                            │
│  ├── Automatic Versioning                                    │
│  ├── Point-in-Time Recovery                                 │
│  └── Audit Trail                                             │
├─────────────────────────────────────────────────────────────┤
│  External Services                                           │
│  ├── Market Data APIs                                        │
│  ├── Settlement Systems                                      │
│  └── ODS Integration                                         │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### **1. REST Controller**
```java
@RestController
@RequestMapping("/api/v1/cashflows")
@Slf4j
public class CashFlowController {
    
    private final CashFlowService cashFlowService;
    private final CalculationStatusService statusService;
    
    @PostMapping("/calculate")
    public ResponseEntity<CashFlowResponse> calculateCashFlows(@RequestBody CashFlowRequest request) {
        log.info("Received cash flow calculation request: {}", request.getRequestId());
        
        try {
            CashFlowResponse response = cashFlowService.calculate(request);
            log.info("Calculation completed for request: {}", request.getRequestId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Calculation failed for request: {}", request.getRequestId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CashFlowResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/calculate/real-time")
    public ResponseEntity<CashFlowResponse> calculateRealTime(@RequestBody CashFlowRequest request) {
        log.info("Received real-time calculation request: {}", request.getRequestId());
        
        try {
            request.setCalculationType(CalculationType.REAL_TIME_PROCESSING);
            CashFlowResponse response = cashFlowService.calculateRealTime(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Real-time calculation failed for request: {}", request.getRequestId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CashFlowResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/calculate/historical")
    public ResponseEntity<CalculationStatus> calculateHistorical(@RequestBody CashFlowRequest request) {
        log.info("Received historical calculation request: {}", request.getRequestId());
        
        try {
            request.setCalculationType(CalculationType.HISTORICAL_RECALCULATION);
            String statusId = cashFlowService.calculateHistoricalAsync(request);
            return ResponseEntity.accepted()
                .body(CalculationStatus.builder()
                    .requestId(request.getRequestId())
                    .status("PROCESSING")
                    .statusUrl("/api/v1/cashflows/status/" + statusId)
                    .build());
        } catch (Exception e) {
            log.error("Historical calculation failed for request: {}", request.getRequestId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CalculationStatus.error(e.getMessage()));
        }
    }
    
    @GetMapping("/status/{requestId}")
    public ResponseEntity<CalculationStatus> getCalculationStatus(@PathVariable String requestId) {
        try {
            CalculationStatus status = statusService.getStatus(requestId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get status for request: {}", requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CalculationStatus.error(e.getMessage()));
        }
    }
    
    @GetMapping("/cashflows/{contractId}")
    public ResponseEntity<List<CashFlow>> getCashFlowsByContract(
            @PathVariable String contractId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) String cashFlowType,
            @RequestParam(required = false) String state) {
        
        try {
            List<CashFlow> cashFlows = cashFlowService.getCashFlowsByContract(
                contractId, fromDate, toDate, cashFlowType, state);
            return ResponseEntity.ok(cashFlows);
        } catch (Exception e) {
            log.error("Failed to get cash flows for contract: {}", contractId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/settlements/pending")
    public ResponseEntity<List<SettlementInstruction>> getPendingSettlements(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String currency) {
        
        try {
            List<SettlementInstruction> settlements = cashFlowService.getPendingSettlements(counterparty, currency);
            return ResponseEntity.ok(settlements);
        } catch (Exception e) {
            log.error("Failed to get pending settlements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### **2. Service Layer**
```java
@Service
@Slf4j
public class CashFlowService {
    
    private final CalculationEngine calculationEngine;
    private final MarketDataService marketDataService;
    private final CashFlowRepository cashFlowRepository;
    private final CalculationStatusService statusService;
    private final ExecutorService virtualThreadExecutor;
    private final ExecutorService cpuThreadExecutor;
    
    public CashFlowService(CalculationEngine calculationEngine,
                          MarketDataService marketDataService,
                          CashFlowRepository cashFlowRepository,
                          CalculationStatusService statusService) {
        this.calculationEngine = calculationEngine;
        this.marketDataService = marketDataService;
        this.cashFlowRepository = cashFlowRepository;
        this.statusService = statusService;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.cpuThreadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    public CashFlowResponse calculate(CashFlowRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting calculation for request: {}", request.getRequestId());
        
        try {
            // Load market data using virtual threads for I/O
            MarketData marketData = loadMarketDataAsync(request);
            log.info("Market data loaded in {}ms", System.currentTimeMillis() - startTime);
            
            // Perform calculations using platform threads for CPU work
            CashFlowResponse response = calculationEngine.calculate(request, marketData);
            log.info("Calculation completed in {}ms", System.currentTimeMillis() - startTime);
            
            // Save results
            saveCashFlows(response);
            log.info("Results saved in {}ms", System.currentTimeMillis() - startTime);
            
            return response;
        } catch (Exception e) {
            log.error("Calculation failed for request: {}", request.getRequestId(), e);
            throw new CashFlowCalculationException("Calculation failed", e);
        }
    }
    
    public CashFlowResponse calculateRealTime(CashFlowRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting real-time calculation for request: {}", request.getRequestId());
        
        try {
            // For real-time, use direct execution for speed
            MarketData marketData = marketDataService.loadMarketData(request);
            CashFlowResponse response = calculationEngine.calculateRealTime(request, marketData);
            
            log.info("Real-time calculation completed in {}ms", System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.error("Real-time calculation failed for request: {}", request.getRequestId(), e);
            throw new CashFlowCalculationException("Real-time calculation failed", e);
        }
    }
    
    public String calculateHistoricalAsync(CashFlowRequest request) {
        String statusId = UUID.randomUUID().toString();
        
        // Start async processing
        CompletableFuture.runAsync(() -> {
            try {
                statusService.updateStatus(statusId, "PROCESSING", 0);
                
                long startTime = System.currentTimeMillis();
                log.info("Starting historical calculation for request: {}", request.getRequestId());
                
                // Load market data
                MarketData marketData = loadMarketDataAsync(request);
                log.info("Market data loaded in {}ms", System.currentTimeMillis() - startTime);
                
                // Perform historical calculation
                CashFlowResponse response = calculationEngine.calculateHistorical(request, marketData);
                log.info("Historical calculation completed in {}ms", System.currentTimeMillis() - startTime);
                
                // Save results
                saveCashFlows(response);
                
                statusService.updateStatus(statusId, "COMPLETED", 100);
                log.info("Historical calculation completed successfully for request: {}", request.getRequestId());
                
            } catch (Exception e) {
                log.error("Historical calculation failed for request: {}", request.getRequestId(), e);
                statusService.updateStatus(statusId, "FAILED", 0, e.getMessage());
            }
        }, virtualThreadExecutor);
        
        return statusId;
    }
    
    private MarketData loadMarketDataAsync(CashFlowRequest request) {
        try {
            return virtualThreadExecutor.submit(() -> marketDataService.loadMarketData(request)).get();
        } catch (Exception e) {
            log.error("Failed to load market data", e);
            throw new MarketDataException("Failed to load market data", e);
        }
    }
    
    private void saveCashFlows(CashFlowResponse response) {
        try {
            cashFlowRepository.saveAll(response.getCashFlows());
        } catch (Exception e) {
            log.error("Failed to save cash flows", e);
            throw new DataPersistenceException("Failed to save cash flows", e);
        }
    }
    
    public List<CashFlow> getCashFlowsByContract(String contractId, LocalDate fromDate, LocalDate toDate, 
                                                String cashFlowType, String state) {
        try {
            return cashFlowRepository.findByContractIdAndDateRange(contractId, fromDate, toDate, cashFlowType, state);
        } catch (Exception e) {
            log.error("Failed to get cash flows for contract: {}", contractId, e);
            throw new DataRetrievalException("Failed to get cash flows", e);
        }
    }
    
    public List<SettlementInstruction> getPendingSettlements(String counterparty, String currency) {
        try {
            return cashFlowRepository.findPendingSettlements(counterparty, currency);
        } catch (Exception e) {
            log.error("Failed to get pending settlements", e);
            throw new DataRetrievalException("Failed to get pending settlements", e);
        }
    }
}
```

### **3. Calculation Engine**
```java
@Component
@Slf4j
public class CalculationEngine {
    
    private final PnLCalculator pnLCalculator;
    private final InterestCalculator interestCalculator;
    private final DividendCalculator dividendCalculator;
    private final ExecutorService cpuThreadExecutor;
    
    public CalculationEngine(PnLCalculator pnLCalculator,
                            InterestCalculator interestCalculator,
                            DividendCalculator dividendCalculator) {
        this.pnLCalculator = pnLCalculator;
        this.interestCalculator = interestCalculator;
        this.dividendCalculator = dividendCalculator;
        this.cpuThreadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    public CashFlowResponse calculate(CashFlowRequest request, MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.info("Starting calculation for {} contracts", request.getContracts().size());
        
        try {
            // Process contracts in parallel using platform threads for CPU work
            List<ContractResult> contractResults = processContractsParallel(request.getContracts(), request, marketData);
            
            // Build response
            CashFlowResponse response = buildResponse(request, contractResults);
            
            log.info("Calculation completed for {} contracts in {}ms", 
                request.getContracts().size(), System.currentTimeMillis() - startTime);
            
            return response;
        } catch (Exception e) {
            log.error("Calculation failed", e);
            throw new CalculationException("Calculation failed", e);
        }
    }
    
    public CashFlowResponse calculateRealTime(CashFlowRequest request, MarketData marketData) {
        // For real-time, use direct execution for speed
        long startTime = System.currentTimeMillis();
        log.info("Starting real-time calculation");
        
        try {
            List<ContractResult> contractResults = processContractsSequential(request.getContracts(), request, marketData);
            CashFlowResponse response = buildResponse(request, contractResults);
            
            log.info("Real-time calculation completed in {}ms", System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.error("Real-time calculation failed", e);
            throw new CalculationException("Real-time calculation failed", e);
        }
    }
    
    public CashFlowResponse calculateHistorical(CashFlowRequest request, MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.info("Starting historical calculation for {} contracts", request.getContracts().size());
        
        try {
            // For historical calculations, use parallel processing
            List<ContractResult> contractResults = processContractsParallel(request.getContracts(), request, marketData);
            CashFlowResponse response = buildResponse(request, contractResults);
            
            log.info("Historical calculation completed for {} contracts in {}ms", 
                request.getContracts().size(), System.currentTimeMillis() - startTime);
            
            return response;
        } catch (Exception e) {
            log.error("Historical calculation failed", e);
            throw new CalculationException("Historical calculation failed", e);
        }
    }
    
    private List<ContractResult> processContractsParallel(List<Contract> contracts, CashFlowRequest request, MarketData marketData) {
        return contracts.parallelStream()
            .map(contract -> {
                try {
                    return calculateContract(contract, request, marketData);
                } catch (Exception e) {
                    log.error("Failed to calculate contract: {}", contract.getContractId(), e);
                    throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
                }
            })
            .collect(Collectors.toList());
    }
    
    private List<ContractResult> processContractsSequential(List<Contract> contracts, CashFlowRequest request, MarketData marketData) {
        return contracts.stream()
            .map(contract -> {
                try {
                    return calculateContract(contract, request, marketData);
                } catch (Exception e) {
                    log.error("Failed to calculate contract: {}", contract.getContractId(), e);
                    throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
                }
            })
            .collect(Collectors.toList());
    }
    
    private ContractResult calculateContract(Contract contract, CashFlowRequest request, MarketData marketData) {
        long startTime = System.currentTimeMillis();
        log.debug("Calculating contract: {}", contract.getContractId());
        
        try {
            // Calculate P&L
            double pnl = pnLCalculator.calculatePnL(contract, marketData);
            
            // Calculate interest
            double interest = interestCalculator.calculateInterest(contract, marketData);
            
            // Calculate dividends
            double dividends = dividendCalculator.calculateDividends(contract, marketData);
            
            // Build contract result
            ContractResult result = ContractResult.builder()
                .contractId(contract.getContractId())
                .underlying(contract.getUnderlying())
                .totalPnl(pnl)
                .totalInterest(interest)
                .totalDividends(dividends)
                .totalCashFlows(pnl + interest + dividends)
                .build();
            
            log.debug("Contract {} calculated in {}ms", contract.getContractId(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Failed to calculate contract: {}", contract.getContractId(), e);
            throw new CalculationException("Failed to calculate contract: " + contract.getContractId(), e);
        }
    }
    
    private CashFlowResponse buildResponse(CashFlowRequest request, List<ContractResult> contractResults) {
        return CashFlowResponse.builder()
            .requestId(request.getRequestId())
            .calculationDate(LocalDate.now())
            .dateRange(request.getDateRange())
            .calculationType(request.getCalculationType())
            .summary(buildSummary(contractResults))
            .contractResults(contractResults)
            .metadata(buildMetadata(request, contractResults))
            .build();
    }
    
    private CalculationSummary buildSummary(List<ContractResult> contractResults) {
        return CalculationSummary.builder()
            .totalContracts(contractResults.size())
            .totalCashFlows(contractResults.stream().mapToInt(r -> r.getCashFlows().size()).sum())
            .totalAmount(contractResults.stream().mapToDouble(ContractResult::getTotalCashFlows).sum())
            .currency("USD")
            .build();
    }
    
    private CalculationMetadata buildMetadata(CashFlowRequest request, List<ContractResult> contractResults) {
        return CalculationMetadata.builder()
            .calculationVersion("1.0")
            .calculationEngine("ConventionalCalculationEngine")
            .processingTimeMs(System.currentTimeMillis())
            .memoryUsageMB(Runtime.getRuntime().totalMemory() / 1024 / 1024)
            .dataSource("HYBRID")
            .build();
    }
}
```

### **4. Market Data Service**
```java
@Service
@Slf4j
public class MarketDataService {
    
    private final WebClient webClient;
    private final MarketDataCache marketDataCache;
    private final ExecutorService virtualThreadExecutor;
    
    public MarketDataService(WebClient webClient, MarketDataCache marketDataCache) {
        this.webClient = webClient;
        this.marketDataCache = marketDataCache;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    public MarketData loadMarketData(CashFlowRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Loading market data for request: {}", request.getRequestId());
        
        try {
            MarketDataStrategy strategy = request.getMarketDataStrategy();
            
            switch (strategy.getMode()) {
                case HYBRID:
                    return loadHybridMarketData(request);
                case SELF_CONTAINED:
                    return loadSelfContainedMarketData(request);
                case ENDPOINTS:
                    return loadFromEndpoints(request);
                default:
                    throw new IllegalArgumentException("Unsupported market data mode: " + strategy.getMode());
            }
        } catch (Exception e) {
            log.error("Failed to load market data for request: {}", request.getRequestId(), e);
            throw new MarketDataException("Failed to load market data", e);
        } finally {
            log.info("Market data loading completed in {}ms", System.currentTimeMillis() - startTime);
        }
    }
    
    private MarketData loadHybridMarketData(CashFlowRequest request) {
        try {
            // Try cache first
            MarketData cached = marketDataCache.get(request.getCacheKey());
            if (cached != null && cached.isValid()) {
                log.info("Using cached market data for request: {}", request.getRequestId());
                return cached;
            }
            
            // Load from endpoints using virtual threads
            return loadFromEndpointsAsync(request);
        } catch (Exception e) {
            log.warn("Hybrid market data loading failed, falling back to self-contained", e);
            return loadSelfContainedMarketData(request);
        }
    }
    
    private MarketData loadFromEndpointsAsync(CashFlowRequest request) {
        try {
            // Load price data
            CompletableFuture<PriceData> priceFuture = CompletableFuture.supplyAsync(() -> {
                return webClient.get()
                    .uri("/prices/{symbol}", request.getUnderlying())
                    .retrieve()
                    .bodyToMono(PriceData.class)
                    .block();
            }, virtualThreadExecutor);
            
            // Load rate data
            CompletableFuture<RateData> rateFuture = CompletableFuture.supplyAsync(() -> {
                return webClient.get()
                    .uri("/rates/{index}", request.getIndex())
                    .retrieve()
                    .bodyToMono(RateData.class)
                    .block();
            }, virtualThreadExecutor);
            
            // Load dividend data
            CompletableFuture<DividendData> dividendFuture = CompletableFuture.supplyAsync(() -> {
                return webClient.get()
                    .uri("/dividends/{symbol}", request.getUnderlying())
                    .retrieve()
                    .bodyToMono(DividendData.class)
                    .block();
            }, virtualThreadExecutor);
            
            // Wait for all futures to complete
            CompletableFuture.allOf(priceFuture, rateFuture, dividendFuture).join();
            
            // Build market data
            MarketData marketData = MarketData.builder()
                .price(priceFuture.get())
                .rate(rateFuture.get())
                .dividends(dividendFuture.get())
                .build();
            
            // Cache the result
            marketDataCache.put(request.getCacheKey(), marketData);
            
            return marketData;
        } catch (Exception e) {
            log.error("Failed to load market data from endpoints", e);
            throw new MarketDataException("Failed to load market data from endpoints", e);
        }
    }
    
    private MarketData loadSelfContainedMarketData(CashFlowRequest request) {
        log.info("Loading self-contained market data for request: {}", request.getRequestId());
        
        MarketDataContainer container = request.getMarketData();
        if (container == null || container.getData() == null) {
            throw new MarketDataException("No self-contained market data provided");
        }
        
        return MarketData.builder()
            .price(extractPriceData(container.getData(), request.getUnderlying()))
            .rate(extractRateData(container.getData(), request.getIndex()))
            .dividends(extractDividendData(container.getData(), request.getUnderlying()))
            .build();
    }
    
    private PriceData extractPriceData(MarketDataContent content, String symbol) {
        return content.getSecurities().stream()
            .filter(security -> security.getSymbol().equals(symbol))
            .findFirst()
            .map(this::buildPriceData)
            .orElseThrow(() -> new MarketDataException("Price data not found for symbol: " + symbol));
    }
    
    private RateData extractRateData(MarketDataContent content, String index) {
        return content.getRates().stream()
            .filter(rate -> rate.getIndex().equals(index))
            .findFirst()
            .map(this::buildRateData)
            .orElseThrow(() -> new MarketDataException("Rate data not found for index: " + index));
    }
    
    private DividendData extractDividendData(MarketDataContent content, String symbol) {
        return content.getDividends().stream()
            .filter(dividend -> dividend.getSymbol().equals(symbol))
            .findFirst()
            .map(this::buildDividendData)
            .orElseThrow(() -> new MarketDataException("Dividend data not found for symbol: " + symbol));
    }
    
    private PriceData buildPriceData(SecurityData security) {
        return PriceData.builder()
            .symbol(security.getSymbol())
            .basePrice(security.getBasePrice())
            .baseDate(security.getBaseDate())
            .changes(security.getChanges())
            .build();
    }
    
    private RateData buildRateData(RateData rate) {
        return RateData.builder()
            .index(rate.getIndex())
            .baseRate(rate.getBaseRate())
            .baseDate(rate.getBaseDate())
            .changes(rate.getChanges())
            .build();
    }
    
    private DividendData buildDividendData(DividendData dividend) {
        return DividendData.builder()
            .symbol(dividend.getSymbol())
            .dividends(dividend.getDividends())
            .build();
    }
}
```

### **5. Repository Layer**
```java
@Repository
@Slf4j
public class CashFlowRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final CashFlowRowMapper cashFlowRowMapper;
    
    public CashFlowRepository(JdbcTemplate jdbcTemplate, CashFlowRowMapper cashFlowRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.cashFlowRowMapper = cashFlowRowMapper;
    }
    
    public void saveAll(List<CashFlow> cashFlows) {
        if (cashFlows.isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO cash_flows (
                cash_flow_id, request_id, contract_id, position_id, lot_id, 
                schedule_id, calculation_date, cash_flow_type, equity_leg_amount,
                interest_leg_amount, total_amount, currency, state,
                equity_unrealized_pnl, equity_realized_pnl, equity_total_pnl,
                equity_dividend_amount, equity_withholding_tax, equity_net_dividend,
                interest_accrued_amount, interest_rate, interest_notional_amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CashFlow cashFlow = cashFlows.get(i);
                ps.setString(1, cashFlow.getCashFlowId());
                ps.setString(2, cashFlow.getRequestId());
                ps.setString(3, cashFlow.getContractId());
                ps.setString(4, cashFlow.getPositionId());
                ps.setString(5, cashFlow.getLotId());
                ps.setString(6, cashFlow.getScheduleId());
                ps.setDate(7, Date.valueOf(cashFlow.getCalculationDate()));
                ps.setString(8, cashFlow.getCashFlowType());
                ps.setBigDecimal(9, cashFlow.getEquityLegAmount());
                ps.setBigDecimal(10, cashFlow.getInterestLegAmount());
                ps.setBigDecimal(11, cashFlow.getTotalAmount());
                ps.setString(12, cashFlow.getCurrency());
                ps.setString(13, cashFlow.getState());
                ps.setBigDecimal(14, cashFlow.getEquityUnrealizedPnl());
                ps.setBigDecimal(15, cashFlow.getEquityRealizedPnl());
                ps.setBigDecimal(16, cashFlow.getEquityTotalPnl());
                ps.setBigDecimal(17, cashFlow.getEquityDividendAmount());
                ps.setBigDecimal(18, cashFlow.getEquityWithholdingTax());
                ps.setBigDecimal(19, cashFlow.getEquityNetDividend());
                ps.setBigDecimal(20, cashFlow.getInterestAccruedAmount());
                ps.setBigDecimal(21, cashFlow.getInterestRate());
                ps.setBigDecimal(22, cashFlow.getInterestNotionalAmount());
            }
            
            @Override
            public int getBatchSize() {
                return cashFlows.size();
            }
        });
        
        log.info("Saved {} cash flows to database", cashFlows.size());
    }
    
    public List<CashFlow> findByContractIdAndDateRange(String contractId, LocalDate fromDate, LocalDate toDate, 
                                                       String cashFlowType, String state) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM cash_flows 
            WHERE contract_id = ? 
            AND calculation_date BETWEEN ? AND ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(contractId);
        params.add(fromDate);
        params.add(toDate);
        
        if (cashFlowType != null) {
            sql.append(" AND cash_flow_type = ?");
            params.add(cashFlowType);
        }
        
        if (state != null) {
            sql.append(" AND state = ?");
            params.add(state);
        }
        
        sql.append(" ORDER BY calculation_date");
        
        return jdbcTemplate.query(sql.toString(), cashFlowRowMapper, params.toArray());
    }
    
    public List<SettlementInstruction> findPendingSettlements(String counterparty, String currency) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM settlement_instructions 
            WHERE status = 'PENDING'
            """);
        
        List<Object> params = new ArrayList<>();
        
        if (counterparty != null) {
            sql.append(" AND counterparty = ?");
            params.add(counterparty);
        }
        
        if (currency != null) {
            sql.append(" AND currency = ?");
            params.add(currency);
        }
        
        sql.append(" ORDER BY settlement_date");
        
        return jdbcTemplate.query(sql.toString(), new SettlementInstructionRowMapper(), params.toArray());
    }
}
```

## Configuration

### **1. Application Properties**
```yaml
# application.yml
spring:
  application:
    name: cash-flow-management-service
  
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=cashflow_db;encrypt=true;trustServerCertificate=true
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.SQLServerDialect
        format_sql: true
  
  webflux:
    webclient:
      max-in-memory-size: 10MB

server:
  port: 8080
  servlet:
    context-path: /api/v1

logging:
  level:
    com.cashflow: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

cashflow:
  calculation:
    max-contracts-per-request: 1000
    max-date-range-days: 1825  # 5 years
    timeout-seconds: 300
  market-data:
    cache-ttl-hours: 24
    max-cache-size-mb: 1000
    external-timeout-seconds: 10
  archival:
    retention-months: 84  # 7 years
    archival-frequency: MONTHLY
```

### **2. Bean Configuration**
```java
@Configuration
@EnableAsync
public class CashFlowConfig {
    
    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Bean
    public ExecutorService cpuThreadExecutor() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(30))
            ))
            .build();
    }
    
    @Bean
    public MarketDataCache marketDataCache() {
        return new MarketDataCache(1000, Duration.ofHours(24));
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

## Error Handling

### **1. Global Exception Handler**
```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CashFlowCalculationException.class)
    public ResponseEntity<ErrorResponse> handleCalculationException(CashFlowCalculationException e) {
        log.error("Calculation error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("CALCULATION_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    @ExceptionHandler(MarketDataException.class)
    public ResponseEntity<ErrorResponse> handleMarketDataException(MarketDataException e) {
        log.error("Market data error", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.builder()
                .errorCode("MARKET_DATA_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        log.error("Validation error", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
```

## Monitoring and Observability

### **1. Health Checks**
```java
@Component
public class CashFlowHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final MarketDataService marketDataService;
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    return Health.up()
                        .withDetail("database", "UP")
                        .withDetail("marketData", "UP")
                        .build();
                }
            }
            
            return Health.down()
                .withDetail("database", "DOWN")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### **2. Metrics**
```java
@Component
public class CashFlowMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter calculationCounter;
    private final Timer calculationTimer;
    private final Gauge activeCalculationsGauge;
    
    public CashFlowMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.calculationCounter = Counter.builder("cashflow.calculations.total")
            .description("Total number of cash flow calculations")
            .register(meterRegistry);
        this.calculationTimer = Timer.builder("cashflow.calculations.duration")
            .description("Duration of cash flow calculations")
            .register(meterRegistry);
        this.activeCalculationsGauge = Gauge.builder("cashflow.calculations.active")
            .description("Number of active calculations")
            .register(meterRegistry, this, CashFlowMetrics::getActiveCalculations);
    }
    
    public void recordCalculation(String calculationType, long durationMs) {
        calculationCounter.increment();
        calculationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("cashflow.calculations.by.type", "type", calculationType).increment();
    }
    
    private double getActiveCalculations() {
        // Implementation to track active calculations
        return 0.0;
    }
}
```

## Conclusion

The conventional approach provides:

### **✅ Benefits**
1. **Simplicity** - Familiar Spring Boot patterns
2. **Maintainability** - Easy to understand and debug
3. **Performance** - Virtual threads for I/O, platform threads for CPU
4. **Reliability** - Proven technology stack
5. **Team Productivity** - Faster development and debugging

### **✅ Key Features**
- **Virtual threads** for I/O operations (market data, database)
- **Platform threads** for CPU-intensive calculations
- **Temporal tables** for automatic versioning
- **Comprehensive error handling** and monitoring
- **Scalable architecture** with clear separation of concerns

### **✅ Performance Targets**
- **Real-time calculations**: <100ms
- **Historical calculations**: <5 minutes for 5-year contracts
- **Throughput**: 10,000+ calculations per minute
- **Memory efficiency**: <4GB for largest calculations

This conventional approach gives you the **best balance** of performance, simplicity, and maintainability for your Cash Flow Management Service.
