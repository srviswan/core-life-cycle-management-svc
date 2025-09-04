# Testing Guide for Cash Flow Management Service

This document provides comprehensive guidance for testing the Cash Flow Management Service using Docker-based integration testing.

## 🧪 Testing Strategy

### Test Types

1. **Unit Tests** - Fast, isolated tests for individual components
2. **Integration Tests** - End-to-end tests with real dependencies
3. **Performance Tests** - Load and stress testing
4. **Docker-Based Testing** - Realistic environment testing

## 🐳 Docker-Based Integration Testing

### Prerequisites

- Docker Desktop installed and running
- Docker Compose available
- Java 21 JDK
- Maven 3.8+

### Quick Start

```bash
# Run all integration tests
./test-integration.sh

# Or run specific test types
./test-integration.sh integration
./test-integration.sh performance
```

### Test Infrastructure

The integration tests use Docker Compose to spin up:

- **SQL Server** (port 1433) - Database for persistence testing
- **Redis** (port 6379) - Cache for performance testing  
- **Kafka** (port 9092) - Messaging for event testing

### Test Configuration

- **`docker-compose.test.yml`** - Test-specific infrastructure
- **`src/test/resources/application-test.yml`** - Test profile configuration
- **`src/test/resources/init-test-db.sql`** - Database initialization

## 🚀 Running Tests

### 1. Unit Tests (Fast)

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=CashFlowCalculationServiceTest

# Run with coverage
mvn test jacoco:report
```

### 2. Integration Tests (Docker Required)

```bash
# Start test infrastructure and run integration tests
./test-integration.sh integration

# Or manually:
docker-compose -f docker-compose.test.yml up -d
mvn test -Dtest=*IntegrationTest -Dspring.profiles.active=test
docker-compose -f docker-compose.test.yml down -v
```

### 3. Performance Tests

```bash
# Run performance tests
./test-integration.sh performance

# Or manually:
docker-compose -f docker-compose.test.yml up -d
mvn test -Dtest=*PerformanceTest -Dspring.profiles.active=test
docker-compose -f docker-compose.test.yml down -v
```

### 4. All Tests

```bash
# Run unit, integration, and performance tests
./test-integration.sh all
```

## 📊 Test Coverage

### Integration Test Coverage

- **REST API Testing** - All endpoints with real HTTP requests
- **Database Persistence** - Verify data is correctly stored
- **Cache Functionality** - Redis cache hit/miss scenarios
- **Concurrent Processing** - Multi-threaded request handling
- **Error Handling** - Graceful failure and recovery
- **Health Checks** - Service health monitoring

### Performance Test Coverage

- **Single Request Performance** - < 100ms target
- **Batch Processing** - 50 requests under 3 seconds
- **Concurrent Load** - 500 requests under 15 seconds
- **Memory Usage** - < 100MB additional under load
- **Cache Performance** - > 95% hit rate for repeated requests

## 🔧 Test Configuration

### Test Profile (`application-test.yml`)

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=cashflow_test
    username: sa
    password: TestPassword123!
  cache:
    redis:
      host: localhost
      port: 6379
      database: 1
  kafka:
    bootstrap-servers: localhost:9092
```

### Docker Test Infrastructure

```yaml
# docker-compose.test.yml
services:
  sqlserver-test:
    image: mcr.microsoft.com/mssql/server:2022-latest
    ports: ["1433:1433"]
    
  redis-test:
    image: redis:7-alpine
    ports: ["6379:6379"]
    
  kafka-test:
    image: confluentinc/cp-kafka:7.4.0
    ports: ["9092:9092"]
```

## 🛠️ Test Scripts

### `test-integration.sh` Commands

```bash
./test-integration.sh start      # Start test infrastructure
./test-integration.sh stop       # Stop test infrastructure
./test-integration.sh status     # Show infrastructure status
./test-integration.sh logs       # Show service logs
./test-integration.sh integration # Run integration tests
./test-integration.sh performance # Run performance tests
./test-integration.sh all        # Run all tests
```

## 📈 Performance Targets

| Test Type | Target | Measurement |
|-----------|--------|-------------|
| Single Calculation | < 100ms | Response time |
| Batch Processing | < 3s | 50 requests |
| Concurrent Load | < 15s | 500 requests |
| Memory Usage | < 100MB | Additional memory |
| Cache Hit Rate | > 95% | Repeated requests |

## 🔍 Debugging Tests

### View Test Logs

```bash
# Show all test service logs
./test-integration.sh logs

# Show specific service logs
docker-compose -f docker-compose.test.yml logs sqlserver-test
docker-compose -f docker-compose.test.yml logs redis-test
docker-compose -f docker-compose.test.yml logs kafka-test
```

### Test Infrastructure Status

```bash
# Check if services are running
./test-integration.sh status

# Check service health
docker-compose -f docker-compose.test.yml ps
```

### Database Inspection

```bash
# Connect to test database
docker-compose -f docker-compose.test.yml exec sqlserver-test /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P TestPassword123! -d cashflow_test

# View tables
SELECT * FROM cashflow.cash_flows;
SELECT * FROM cashflow.settlement_instructions;
SELECT * FROM cashflow.calculation_requests;
```

## 🐛 Common Issues

### Docker Issues

```bash
# Docker not running
Error: Docker is not running. Please start Docker and try again.

# Solution: Start Docker Desktop

# Port conflicts
Error: Port 1433 is already in use

# Solution: Stop conflicting services or change ports in docker-compose.test.yml
```

### Test Failures

```bash
# Database connection issues
Error: SQL Server failed to start within timeout

# Solution: Check Docker resources, increase timeout in test script

# Cache connection issues  
Error: Redis connection failed

# Solution: Verify Redis container is healthy, check network connectivity
```

### Performance Issues

```bash
# Tests timing out
Error: Performance targets not met

# Solution: 
# 1. Increase Docker resources (CPU/Memory)
# 2. Adjust performance targets in test configuration
# 3. Check system load during testing
```

## 📝 Writing New Tests

### Integration Test Template

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class MyIntegrationTest {

    @Container
    static MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(
        DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
        .acceptLicense()
        .withPassword("TestPassword123!");

    @Test
    void shouldTestSomething() {
        // Given
        // When  
        // Then
    }
}
```

### Performance Test Template

```java
@Test
void shouldMeetPerformanceTarget() {
    // Given
    long startTime = System.currentTimeMillis();
    
    // When
    // Perform operation
    
    // Then
    long duration = System.currentTimeMillis() - startTime;
    assertThat(duration).isLessThan(100); // < 100ms
}
```

## 🎯 Best Practices

1. **Isolation** - Each test should be independent
2. **Cleanup** - Always clean up test data
3. **Realistic Data** - Use realistic test data
4. **Performance** - Monitor test execution time
5. **Logging** - Use appropriate log levels for debugging
6. **Configuration** - Use test-specific configuration
7. **Docker** - Leverage containers for realistic testing

## 📊 Monitoring Test Results

### Test Reports

```bash
# Generate test reports
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Performance Metrics

```bash
# Monitor during performance tests
curl http://localhost:8080/actuator/metrics

# Key metrics:
# - http_server_requests_seconds_count
# - jvm_memory_used_bytes
# - cache_gets_total
# - cache_miss_total
```

---

**Note**: Always ensure Docker Desktop is running before executing integration tests. The test infrastructure requires significant resources, so ensure adequate CPU and memory allocation.
