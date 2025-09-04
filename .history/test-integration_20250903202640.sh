#!/bin/bash

# Integration Test Script for Cash Flow Management Service
# This script sets up the test environment and runs integration tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to check if Docker Compose is available
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose and try again."
        exit 1
    fi
}

# Function to stop and remove existing containers
cleanup_existing() {
    print_step "Cleaning up existing test containers..."
    
    # Stop and remove test containers
    docker-compose -f docker-compose.test.yml down -v 2>/dev/null || true
    
    # Stop and remove main containers if they conflict with test ports
    docker-compose down -v 2>/dev/null || true
    
    print_status "Cleanup completed"
}

# Function to start test infrastructure
start_test_infrastructure() {
    print_step "Starting test infrastructure..."
    
    # Start test services
    docker-compose -f docker-compose.test.yml up -d
    
    # Wait for services to be healthy
    print_status "Waiting for services to be ready..."
    
    # Wait for SQL Server
    print_status "Waiting for SQL Server..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if docker-compose -f docker-compose.test.yml exec sqlserver-test /opt/mssql-tools/bin/sqlcmd -S localhost,1433 -U sa -P TestPassword123! -Q "SELECT 1" > /dev/null 2>&1; then
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done
    
    if [ $timeout -le 0 ]; then
        print_error "SQL Server failed to start within timeout"
        exit 1
    fi
    
    # Wait for Redis
    print_status "Waiting for Redis..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker-compose -f docker-compose.test.yml exec redis-test redis-cli ping > /dev/null 2>&1; then
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        print_error "Redis failed to start within timeout"
        exit 1
    fi
    
    # Wait for Kafka
    print_status "Waiting for Kafka..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if docker-compose -f docker-compose.test.yml exec kafka-test kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done
    
    if [ $timeout -le 0 ]; then
        print_error "Kafka failed to start within timeout"
        exit 1
    fi
    
    print_status "All test services are ready"
}

# Function to run integration tests
run_integration_tests() {
    print_step "Running integration tests..."
    
    # Run integration tests
    mvn test -Dtest=*IntegrationTest -Dspring.profiles.active=test
    
    if [ $? -eq 0 ]; then
        print_status "Integration tests passed!"
    else
        print_error "Integration tests failed!"
        exit 1
    fi
}

# Function to run performance tests
run_performance_tests() {
    print_step "Running performance tests..."
    
    # Run performance tests
    mvn test -Dtest=*PerformanceTest -Dspring.profiles.active=test
    
    if [ $? -eq 0 ]; then
        print_status "Performance tests passed!"
    else
        print_error "Performance tests failed!"
        exit 1
    fi
}

# Function to stop test infrastructure
stop_test_infrastructure() {
    print_step "Stopping test infrastructure..."
    
    docker-compose -f docker-compose.test.yml down -v
    
    print_status "Test infrastructure stopped"
}

# Function to show test logs
show_logs() {
    print_step "Showing test service logs..."
    
    echo "=== SQL Server Logs ==="
    docker-compose -f docker-compose.test.yml logs sqlserver-test
    
    echo "=== Redis Logs ==="
    docker-compose -f docker-compose.test.yml logs redis-test
    
    echo "=== Kafka Logs ==="
    docker-compose -f docker-compose.test.yml logs kafka-test
}

# Function to show test status
show_status() {
    print_step "Test infrastructure status:"
    
    docker-compose -f docker-compose.test.yml ps
}

# Main script logic
case "${1:-all}" in
    start)
        check_docker
        check_docker_compose
        cleanup_existing
        start_test_infrastructure
        show_status
        ;;
    stop)
        stop_test_infrastructure
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    integration)
        check_docker
        check_docker_compose
        cleanup_existing
        start_test_infrastructure
        run_integration_tests
        stop_test_infrastructure
        ;;
    performance)
        check_docker
        check_docker_compose
        cleanup_existing
        start_test_infrastructure
        run_performance_tests
        stop_test_infrastructure
        ;;
    all)
        check_docker
        check_docker_compose
        cleanup_existing
        start_test_infrastructure
        run_integration_tests
        run_performance_tests
        stop_test_infrastructure
        ;;
    *)
        echo "Usage: $0 {start|stop|status|logs|integration|performance|all}"
        echo "  start       - Start test infrastructure only"
        echo "  stop        - Stop test infrastructure"
        echo "  status      - Show test infrastructure status"
        echo "  logs        - Show test service logs"
        echo "  integration - Run integration tests"
        echo "  performance - Run performance tests"
        echo "  all         - Run all tests (default)"
        exit 1
        ;;
esac
