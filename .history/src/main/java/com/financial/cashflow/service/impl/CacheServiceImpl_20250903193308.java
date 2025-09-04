package com.financial.cashflow.service.impl;

import com.financial.cashflow.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of CacheService for caching operations.
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    private final CacheManager cacheManager;

    public CacheServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            Cache cache = getCache();
            Cache.ValueWrapper wrapper = cache.get(key);
            
            if (wrapper != null) {
                Object value = wrapper.get();
                if (type.isInstance(value)) {
                    log.debug("Cache hit for key: {}", key);
                    return type.cast(value);
                } else {
                    log.warn("Cache value type mismatch for key: {}. Expected: {}, Found: {}", 
                        key, type.getSimpleName(), value.getClass().getSimpleName());
                }
            } else {
                log.debug("Cache miss for key: {}", key);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error retrieving from cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> boolean put(String key, T value) {
        try {
            Cache cache = getCache();
            cache.put(key, value);
            log.debug("Successfully cached value for key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Error storing in cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get a value from cache with a specific cache name.
     */
    public <T> T get(String cacheName, String key, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found", cacheName);
                return null;
            }
            
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                Object value = wrapper.get();
                if (type.isInstance(value)) {
                    log.debug("Cache hit for cache: {}, key: {}", cacheName, key);
                    return type.cast(value);
                }
            }
            
            log.debug("Cache miss for cache: {}, key: {}", cacheName, key);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving from cache: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    /**
     * Put a value in cache with a specific cache name.
     */
    public <T> boolean put(String cacheName, String key, T value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found", cacheName);
                return false;
            }
            
            cache.put(key, value);
            log.debug("Successfully cached value for cache: {}, key: {}", cacheName, key);
            return true;
        } catch (Exception e) {
            log.error("Error storing in cache: {}, key: {}", cacheName, key, e);
            return false;
        }
    }

    /**
     * Put a value in cache with TTL (Time To Live).
     */
    public <T> boolean put(String key, T value, long ttl, TimeUnit timeUnit) {
        try {
            Cache cache = getCache();
            // Note: TTL support depends on the cache implementation
            // For Redis, this would be handled by the Redis configuration
            // For Caffeine, this would be handled by the cache configuration
            cache.put(key, value);
            log.debug("Successfully cached value for key: {} with TTL: {} {}", key, ttl, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Error storing in cache for key: {} with TTL", key, e);
            return false;
        }
    }

    /**
     * Remove a value from cache.
     */
    public boolean evict(String key) {
        try {
            Cache cache = getCache();
            cache.evict(key);
            log.debug("Successfully evicted cache entry for key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Error evicting from cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Clear all entries from cache.
     */
    public boolean clear() {
        try {
            Cache cache = getCache();
            cache.clear();
            log.info("Successfully cleared all cache entries");
            return true;
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return false;
        }
    }

    /**
     * Check if a key exists in cache.
     */
    public boolean exists(String key) {
        try {
            Cache cache = getCache();
            Cache.ValueWrapper wrapper = cache.get(key);
            boolean exists = wrapper != null;
            log.debug("Cache key '{}' exists: {}", key, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking cache existence for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get cache statistics (if supported by the cache implementation).
     */
    public CacheStats getStats() {
        try {
            // This is a simplified implementation
            // In a real implementation, you would get actual statistics from the cache
            return CacheStats.builder()
                .cacheName("cash-flow-cache")
                .hitCount(0) // Would be actual hit count
                .missCount(0) // Would be actual miss count
                .loadCount(0) // Would be actual load count
                .build();
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return null;
        }
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache("cash-flow-cache");
        if (cache == null) {
            throw new IllegalStateException("Cache 'cash-flow-cache' not found. Check cache configuration.");
        }
        return cache;
    }

    /**
     * Cache statistics holder.
     */
    public static class CacheStats {
        private String cacheName;
        private long hitCount;
        private long missCount;
        private long loadCount;

        public static CacheStatsBuilder builder() {
            return new CacheStatsBuilder();
        }

        public static class CacheStatsBuilder {
            private CacheStats stats = new CacheStats();

            public CacheStatsBuilder cacheName(String cacheName) {
                stats.cacheName = cacheName;
                return this;
            }

            public CacheStatsBuilder hitCount(long hitCount) {
                stats.hitCount = hitCount;
                return this;
            }

            public CacheStatsBuilder missCount(long missCount) {
                stats.missCount = missCount;
                return this;
            }

            public CacheStatsBuilder loadCount(long loadCount) {
                stats.loadCount = loadCount;
                return this;
            }

            public CacheStats build() {
                return stats;
            }
        }

        // Getters
        public String getCacheName() { return cacheName; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getLoadCount() { return loadCount; }

        public double getHitRate() {
            long totalRequests = hitCount + missCount;
            return totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{cacheName='%s', hitCount=%d, missCount=%d, loadCount=%d, hitRate=%.2f}",
                cacheName, hitCount, missCount, loadCount, getHitRate());
        }
    }
}
