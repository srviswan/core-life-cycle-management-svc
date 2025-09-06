package com.financial.cashflow.service;

import com.financial.cashflow.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Market Data Cache for storing and retrieving market data
 */
@Component
@Slf4j
public class MarketDataCache {
    
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public MarketDataCache() {
        // Schedule cleanup every hour
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Get market data from cache
     */
    public MarketData get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for key: {}", key);
            return entry.getData();
        }
        
        if (entry != null) {
            cache.remove(key);
            log.debug("Cache entry expired for key: {}", key);
        }
        
        return null;
    }
    
    /**
     * Put market data in cache
     */
    public void put(String key, MarketData data) {
        CacheEntry entry = new CacheEntry(data, LocalDateTime.now().plusHours(24));
        cache.put(key, entry);
        log.debug("Cached market data for key: {}", key);
    }
    
    /**
     * Remove entry from cache
     */
    public void remove(String key) {
        cache.remove(key);
        log.debug("Removed cache entry for key: {}", key);
    }
    
    /**
     * Clear all cache entries
     */
    public void clear() {
        cache.clear();
        log.info("Cleared all cache entries");
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Cleanup expired entries
     */
    private void cleanup() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int finalSize = cache.size();
        
        if (initialSize != finalSize) {
            log.info("Cleaned up {} expired cache entries", initialSize - finalSize);
        }
    }
    
    /**
     * Cache entry wrapper
     */
    private static class CacheEntry {
        private final MarketData data;
        private final LocalDateTime expiresAt;
        
        public CacheEntry(MarketData data, LocalDateTime expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
        
        public MarketData getData() {
            return data;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
