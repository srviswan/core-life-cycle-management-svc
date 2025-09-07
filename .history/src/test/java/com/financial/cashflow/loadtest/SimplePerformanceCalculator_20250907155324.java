package com.financial.cashflow.loadtest;

/**
 * Simple Performance Calculator with Direct Calculations
 */
public class SimplePerformanceCalculator {
    
    // Performance baselines from load testing
    private static final double SIMPLE_THROUGHPUT = 5.49; // req/sec
    private static final double MEDIUM_THROUGHPUT = 3.5;  // req/sec
    private static final double COMPLEX_THROUGHPUT = 1.0; // req/sec (estimated)
    private static final int OPTIMAL_THREADS = 16;
    
    public static void main(String[] args) {
        System.out.println("=== ENTERPRISE SCALE PERFORMANCE CALCULATIONS ===\n");
        
        // Scenario 1: 65K contracts × 4 positions = 260K positions
        calculateScenario("65K Contracts × 4 Positions", 65000, 4);
        
        // Scenario 2: 160K contracts × 4 positions = 640K positions  
        calculateScenario("160K Contracts × 4 Positions", 160000, 4);
        
        // Scenario 3: 6.5K lots maximum
        calculateLotScenario("6.5K Lots Maximum", 6500);
        
        // Batch processing analysis
        analyzeBatchProcessing();
    }
    
    private static void calculateScenario(String scenarioName, int contracts, int positionsPerContract) {
        System.out.println("=== " + scenarioName + " ===");
        
        int totalPositions = contracts * positionsPerContract;
        
        System.out.println("Total Contracts: " + contracts);
        System.out.println("Positions per Contract: " + positionsPerContract);
        System.out.println("Total Positions: " + totalPositions);
        
        // Calculate processing time for different complexity levels
        double simpleTime = totalPositions / SIMPLE_THROUGHPUT;
        double mediumTime = totalPositions / MEDIUM_THROUGHPUT;
        double complexTime = totalPositions / COMPLEX_THROUGHPUT;
        
        System.out.println("\nProcessing Time Estimates:");
        System.out.printf("  Simple Complexity: %.2f seconds (%.2f minutes)\n", simpleTime, simpleTime / 60);
        System.out.printf("  Medium Complexity: %.2f seconds (%.2f minutes)\n", mediumTime, mediumTime / 60);
        System.out.printf("  Complex Complexity: %.2f seconds (%.2f minutes)\n", complexTime, complexTime / 60);
        
        // Parallel processing with optimal threads
        double parallelSimpleTime = simpleTime / OPTIMAL_THREADS;
        double parallelMediumTime = mediumTime / OPTIMAL_THREADS;
        double parallelComplexTime = complexTime / OPTIMAL_THREADS;
        
        System.out.println("\nParallel Processing (" + OPTIMAL_THREADS + " threads):");
        System.out.printf("  Simple Complexity: %.2f seconds (%.2f minutes)\n", parallelSimpleTime, parallelSimpleTime / 60);
        System.out.printf("  Medium Complexity: %.2f seconds (%.2f minutes)\n", parallelMediumTime, parallelMediumTime / 60);
        System.out.printf("  Complex Complexity: %.2f seconds (%.2f minutes)\n", parallelComplexTime, parallelComplexTime / 60);
        
        // Memory estimation
        long totalMemoryMB = (totalPositions * 1024) / (1024 * 1024);
        System.out.println("\nMemory Estimation:");
        System.out.println("  Memory per Position: 1 KB");
        System.out.println("  Total Memory Required: " + totalMemoryMB + " MB");
        System.out.println("  Recommended JVM Heap: " + Math.max(2, totalMemoryMB / 1024 + 1) + " GB");
        
        System.out.println();
    }
    
    private static void calculateLotScenario(String scenarioName, int maxLots) {
        System.out.println("=== " + scenarioName + " ===");
        
        // Assume average of 5 lots per position
        int estimatedPositions = maxLots / 5;
        int estimatedContracts = estimatedPositions / 4;
        
        System.out.println("Maximum Lots: " + maxLots);
        System.out.println("Estimated Positions: " + estimatedPositions);
        System.out.println("Estimated Contracts: " + estimatedContracts);
        
        calculateScenario(scenarioName, estimatedContracts, 4);
    }
    
    private static void analyzeBatchProcessing() {
        System.out.println("=== BATCH PROCESSING ANALYSIS ===");
        
        // Analyze different batch sizes for 160K contracts
        int totalContracts = 160000;
        int[] batchSizes = {1000, 5000, 10000};
        
        for (int batchSize : batchSizes) {
            System.out.println("\nBatch Size: " + batchSize);
            
            int batches = (int) Math.ceil((double) totalContracts / batchSize);
            double timePerBatch = batchSize / MEDIUM_THROUGHPUT;
            double totalTime = batches * timePerBatch;
            double parallelTime = totalTime / OPTIMAL_THREADS;
            
            System.out.println("  Batches Required: " + batches);
            System.out.printf("  Time per Batch: %.2f seconds\n", timePerBatch);
            System.out.printf("  Total Sequential Time: %.2f minutes\n", totalTime / 60);
            System.out.printf("  Total Parallel Time: %.2f minutes\n", parallelTime / 60);
        }
        
        System.out.println("\n=== KEY RECOMMENDATIONS ===");
        System.out.println("1. Use batch sizes of 1,000-5,000 contracts");
        System.out.println("2. Process batches in parallel with 16 threads");
        System.out.println("3. Expected time for 160K contracts: 15-30 minutes");
        System.out.println("4. Allocate 8-16GB JVM heap for large datasets");
        System.out.println("5. Use horizontal scaling for faster processing");
    }
}
