package com.financial.cashflow;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Comprehensive test suite for real-time cash flow calculations
 * Runs all unit tests, integration tests, and performance tests
 */
@Suite
@SuiteDisplayName("Real-Time Cash Flow Calculation Test Suite")
@SelectClasses({
        // Unit Tests
        com.financial.cashflow.calculator.InterestCalculatorTest.class,
        com.financial.cashflow.calculator.PnLCalculatorTest.class,
        com.financial.cashflow.calculator.DividendCalculatorTest.class,
        
        // Integration Tests
        com.financial.cashflow.controller.CashFlowControllerIntegrationTest.class,
        
        // Performance Tests
        com.financial.cashflow.performance.RealTimeCalculationPerformanceTest.class,
        
        // Application Tests
        CashFlowManagementServiceApplicationTests.class
})
public class RealTimeCashFlowTestSuite {
    // This class serves as a test suite container
    // All tests are selected via @SelectClasses annotation
}
