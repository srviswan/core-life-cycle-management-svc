#!/bin/bash

# Load Test Script for Cash Flow Management Service
# This script runs comprehensive load tests to determine optimal thread pool sizing

echo "=========================================="
echo "Cash Flow Management Service Load Testing"
echo "=========================================="

# Check if application is running
echo "Checking if application is running..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ Application is not running on localhost:8080"
    echo "Please start the application first using: docker-compose up -d"
    exit 1
fi

echo "✅ Application is running"

# Compile and run load tests
echo "Compiling load test classes..."
mvn test-compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

echo "✅ Compilation successful"

# Run load tests
echo "Starting load tests..."
echo "This may take several minutes..."

# Run the load test runner
mvn exec:java -Dexec.mainClass="com.financial.cashflow.loadtest.LoadTestRunner" -q

if [ $? -eq 0 ]; then
    echo "✅ Load tests completed successfully"
else
    echo "❌ Load tests failed"
    exit 1
fi

echo "=========================================="
echo "Load testing completed"
echo "=========================================="
