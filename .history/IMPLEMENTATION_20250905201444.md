# Cash Flow Management Service - Conventional Implementation

## Overview

This implementation follows the **conventional Spring Boot approach** as outlined in the `cash-flow-conventional-implementation.md` document. It provides a robust, maintainable, and performant solution for cash flow calculations using:

- **Spring Boot 3.2.0** with Java 21
- **Virtual threads** for I/O operations
- **Platform threads** for CPU-intensive calculations
- **MS SQL Server** with temporal tables
- **Synchronous APIs** with comprehensive error handling

## Architecture

### Service Stack
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

## Key Components

### 1. REST Controller (`CashFlowController`)
- **Synchronous APIs** following conventional Spring MVC pattern
- **Real-time calculations** for immediate responses
- **Historical calculations** with async processing
- **Status tracking** for long-running operations
- **Comprehensive error handling**

### 2. Service Layer (`CashFlowService`)
- **Virtual threads** for I/O operations (market data, database)
- **Platform threads** for CPU-intensive calculations
- **Hybrid market data strategy** (cache + endpoints + self-contained)
- **Async processing** for historical calculations

### 3. Calculation Engine (`CalculationEngine`)
- **Parallel processing** for multiple contracts
- **Sequential processing** for real-time calculations
- **Modular calculators** (P&L, Interest, Dividends)
- **Performance optimization** with platform threads

### 4. Market Data Service (`MarketDataService`)
- **Hybrid strategy** supporting multiple data sources
- **Caching** with TTL and cleanup
- **Async loading** using virtual threads
- **Fallback mechanisms** for reliability

### 5. Repository Layer (`CashFlowRepository`)
- **JDBC Template** for direct SQL operations
- **Batch operations** for performance
- **Temporal tables** for versioning
- **Comprehensive querying** with filters

## Features

### ✅ Core Features
- **Real-time calculations**: <100ms response time
- **Historical calculations**: Async processing with status tracking
- **Market data integration**: Hybrid strategy with caching
- **Settlement management**: Pending settlements tracking
- **Comprehensive monitoring**: Health checks and metrics

### ✅ Performance Features
- **Virtual threads** for I/O operations
- **Platform threads** for CPU work
- **Parallel processing** for batch calculations
- **Connection pooling** with HikariCP
- **Batch database operations**

### ✅ Reliability Features
- **Temporal tables** for data versioning
- **Comprehensive error handling**
- **Health checks** and monitoring
- **Graceful degradation** with fallbacks
- **Input validation** and sanitization

## API Endpoints

### Calculation Endpoints
- `POST /api/v1/cashflows/calculate` - Synchronous calculation
- `POST /api/v1/cashflows/calculate/real-time` - Real-time calculation
- `POST /api/v1/cashflows/calculate/historical` - Historical calculation (async)

### Status and Data Endpoints
- `GET /api/v1/cashflows/status/{requestId}` - Get calculation status
- `GET /api/v1/cashflows/cashflows/{contractId}` - Get cash flows by contract
- `GET /api/v1/cashflows/settlements/pending` - Get pending settlements
- `GET /api/v1/cashflows/health` - Health check

## Configuration

### Application Properties
```yaml
spring:
  application:
    name: cash-flow-management-service
  
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=cashflow_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver

cashflow:
  calculation:
    max-contracts-per-request: 1000
    max-date-range-days: 1825  # 5 years
    timeout-seconds: 300
  market-data:
    cache-ttl-hours: 24
    max-cache-size-mb: 1000
    external-timeout-seconds: 10
```

## Database Schema

### Tables
- **`cash_flows`** - Main cash flow data with temporal support
- **`calculation_requests`** - Request tracking and status
- **`settlement_instructions`** - Settlement management
- **`market_data_cache`** - Market data caching

### Features
- **Temporal tables** for automatic versioning
- **Indexes** for performance optimization
- **Stored procedures** for maintenance
- **Cleanup jobs** for cache management

## Monitoring and Observability

### Health Checks
- **Database connectivity** monitoring
- **Service status** tracking
- **Component health** verification

### Metrics
- **Calculation metrics**: Count, duration, active calculations
- **Market data metrics**: Requests, duration, source tracking
- **Error metrics**: Error types and codes
- **Prometheus integration** for monitoring

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- MS SQL Server 2019+
- Docker (optional)

### Running the Application

1. **Start the database**:
   ```bash
   docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=YourStrong@Passw0rd" \
     -p 1433:1433 --name sqlserver \
     mcr.microsoft.com/mssql/server:2019-latest
   ```

2. **Set environment variables**:
   ```bash
   export DB_USERNAME=sa
   export DB_PASSWORD=YourStrong@Passw0rd
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**:
   - API: http://localhost:8080/api/v1/cashflows
   - Health: http://localhost:8080/api/v1/cashflows/health
   - Metrics: http://localhost:8080/actuator/prometheus

### Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify -Pintegration-test
```

## Performance Targets

- **Real-time calculations**: <100ms
- **Historical calculations**: <5 minutes for 5-year contracts
- **Throughput**: 10,000+ calculations per minute
- **Memory efficiency**: <4GB for largest calculations
- **Availability**: 99.9% uptime

## Benefits of Conventional Approach

### ✅ Advantages
1. **Simplicity** - Familiar Spring Boot patterns
2. **Maintainability** - Easy to understand and debug
3. **Performance** - Virtual threads for I/O, platform threads for CPU
4. **Reliability** - Proven technology stack
5. **Team Productivity** - Faster development and debugging

### ✅ Key Features
- **Virtual threads** for I/O operations (market data, database)
- **Platform threads** for CPU-intensive calculations
- **Temporal tables** for automatic versioning
- **Comprehensive error handling** and monitoring
- **Scalable architecture** with clear separation of concerns

This conventional implementation provides the **best balance** of performance, simplicity, and maintainability for the Cash Flow Management Service.
