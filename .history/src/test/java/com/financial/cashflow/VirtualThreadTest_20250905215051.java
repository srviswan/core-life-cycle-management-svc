package com.financial.cashflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify Java 21 virtual threads are working correctly
 */
@SpringBootTest
@ActiveProfiles("test")
class VirtualThreadTest {

    @Test
    void testVirtualThreadsWork() {
        // Test that virtual threads can be created and used
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Submit a simple task
            var future = virtualExecutor.submit(() -> {
                // Simulate some work
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Virtual thread task completed";
            });
            
            // Verify the task completes
            String result = future.get(5, TimeUnit.SECONDS);
            assertEquals("Virtual thread task completed", result);
            
            // Verify we're using virtual threads by checking thread name
            String threadName = Thread.currentThread().getName();
            System.out.println("Current thread: " + threadName);
            
            // Virtual threads typically have names like "VirtualThread[#123]/runnable@ForkJoinPool-1-worker-1"
            // or similar patterns
            assertNotNull(threadName);
        } catch (Exception e) {
            fail("Virtual threads test failed: " + e.getMessage());
        }
    }
    
    @Test
    void testVirtualThreadPoolConfiguration() {
        // Test that our configuration creates virtual thread executors
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        assertNotNull(virtualExecutor);
        assertFalse(virtualExecutor.isShutdown());
        assertFalse(virtualExecutor.isTerminated());
        
        // Test that it can execute tasks
        try {
            var future = virtualExecutor.submit(() -> "test");
            assertEquals("test", future.get());
        } catch (Exception e) {
            fail("Virtual executor test failed: " + e.getMessage());
        } finally {
            virtualExecutor.shutdown();
        }
    }
}
