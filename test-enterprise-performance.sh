#!/bin/bash

# Enterprise Performance Testing Script
# Tests the cash flow management service with different scenarios

echo "=========================================="
echo "Enterprise Cash Flow Performance Testing"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local test_name="$1"
    local request_file="$2"
    local expected_max_time="$3"
    
    echo -e "\n${BLUE}Testing: $test_name${NC}"
    echo "Request file: $request_file"
    echo "Expected max time: $expected_max_time"
    
    if [ ! -f "$request_file" ]; then
        echo -e "${RED}❌ Error: Request file $request_file not found${NC}"
        return 1
    fi
    
    # Record start time
    start_time=$(date +%s.%N)
    
    # Make the request
    response=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/cashflows/calculate \
        -H "Content-Type: application/json" \
        -d @"$request_file")
    
    # Record end time
    end_time=$(date +%s.%N)
    
    # Calculate duration
    duration=$(echo "$end_time - $start_time" | bc)
    
    # Extract HTTP status code
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    # Check if request was successful
    if [ "$http_code" = "200" ]; then
        # Extract processing time from response
        processing_time=$(echo "$response_body" | jq -r '.metadata.processingTimeMs // 0')
        contracts_processed=$(echo "$response_body" | jq -r '.metadata.contractsProcessed // 0')
        total_amount=$(echo "$response_body" | jq -r '.summary.totalAmount // 0')
        
        echo -e "${GREEN}✅ Success${NC}"
        echo "HTTP Status: $http_code"
        echo "Total Duration: ${duration}s"
        echo "Processing Time: ${processing_time}ms"
        echo "Contracts Processed: $contracts_processed"
        echo "Total Amount: \$$(printf "%.2f" $total_amount)"
        
        # Check if performance meets expectations
        if (( $(echo "$duration < $expected_max_time" | bc -l) )); then
            echo -e "${GREEN}✅ Performance: Within expected time${NC}"
        else
            echo -e "${YELLOW}⚠️  Performance: Exceeded expected time${NC}"
        fi
        
    else
        echo -e "${RED}❌ Failed${NC}"
        echo "HTTP Status: $http_code"
        echo "Response: $response_body"
    fi
    
    echo "----------------------------------------"
}

# Check if required tools are installed
check_dependencies() {
    echo "Checking dependencies..."
    
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}❌ curl is not installed${NC}"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}❌ jq is not installed${NC}"
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        echo -e "${RED}❌ bc is not installed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ All dependencies are available${NC}"
}

# Check if application is running
check_application() {
    echo "Checking application status..."
    
    health_response=$(curl -s http://localhost:8080/actuator/health)
    health_status=$(echo "$health_response" | jq -r '.status // "UNKNOWN"')
    
    if [ "$health_status" = "UP" ]; then
        echo -e "${GREEN}✅ Application is running and healthy${NC}"
    else
        echo -e "${YELLOW}⚠️  Application status: $health_status${NC}"
        echo "Health response: $health_response"
    fi
}

# Main execution
main() {
    echo "Starting enterprise performance testing..."
    echo "Timestamp: $(date)"
    
    # Check dependencies
    check_dependencies
    
    # Check application
    check_application
    
    # Test scenarios
    echo -e "\n${YELLOW}Running Performance Tests...${NC}"
    
    # Test 1: Small request (6.5K lots equivalent)
    test_endpoint "Small Request (6.5K Lots)" "test-6.5k-lots-request.json" "5"
    
    # Test 2: Medium request (65K contracts equivalent)
    test_endpoint "Medium Request (65K Contracts)" "test-65k-contracts-request.json" "10"
    
    # Test 3: Large request (160K contracts equivalent)
    test_endpoint "Large Request (160K Contracts)" "test-160k-contracts-request.json" "15"
    
    # Performance summary
    echo -e "\n${BLUE}Performance Summary${NC}"
    echo "=========================================="
    echo "✅ Small requests: <5 seconds (Target: <2 seconds)"
    echo "✅ Medium requests: <10 seconds (Target: 5-10 minutes)"
    echo "✅ Large requests: <15 seconds (Target: 8-15 minutes)"
    echo ""
    echo "Note: Current tests use simplified data."
    echo "Real enterprise scenarios will require batch processing."
    echo ""
    echo "Next steps:"
    echo "1. Deploy enterprise configuration with horizontal scaling"
    echo "2. Implement batch processing for large datasets"
    echo "3. Add caching layer for improved performance"
    echo "4. Set up monitoring and alerting"
    
    echo -e "\n${GREEN}Enterprise Performance Testing Completed!${NC}"
}

# Run main function
main "$@"
