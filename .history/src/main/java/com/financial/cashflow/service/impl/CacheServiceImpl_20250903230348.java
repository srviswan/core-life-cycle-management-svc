package com.financial.cashflow.service.impl;

import com.financial.cashflow.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of CacheService using Redis.
 * Uses virtual threads for I/O operations.
 * Provides caching with TTL and serialization support.
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit for key: {}", key);
                return type.cast(value);
            } else {
                log.debug("Cache miss for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to get value from cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> boolean put(String key, T value) {
        try {
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
            log.debug("Successfully cached value for key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to cache value for key: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean evict(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Evicted cache for key: {}", key);
            return result != null && result;
        } catch (Exception e) {
            log.warn("Failed to evict cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get value from cache asynchronously using virtual threads.
     */
    public <T> CompletableFuture<T> getAsync(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> get(key, type), virtualThreadExecutor);
    }

    /**
     * Put value in cache asynchronously using virtual threads.
     */
    public <T> CompletableFuture<Boolean> putAsync(String key, T value) {
        return CompletableFuture.supplyAsync(() -> put(key, value), virtualThreadExecutor);
    }

    /**
     * Evict value from cache asynchronously using virtual threads.
     */
    public CompletableFuture<Boolean> evictAsync(String key) {
        return CompletableFuture.supplyAsync(() -> evict(key), virtualThreadExecutor);
    }

    /**
     * Put value in cache with custom TTL.
     */
    public <T> boolean put(String key, T value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Successfully cached value for key: {} with TTL: {}", key, ttl);
            return true;
        } catch (Exception e) {
            log.error("Failed to cache value for key: {} with TTL: {}", key, ttl, e);
            return false;
        }
    }

    /**
     * Check if key exists in cache.
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            log.warn("Failed to check existence for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get TTL for a key.
     */
    public Duration getTtl(String key) {
        try {
            Long ttlSeconds = redisTemplate.getExpire(key);
            if (ttlSeconds != null && ttlSeconds > 0) {
                return Duration.ofSeconds(ttlSeconds);
            }
            return Duration.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get TTL for key: {}", key, e);
            return Duration.ZERO;
        }
    }

    /**
     * Clear all cache entries.
     */
    public boolean clearAll() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("Cleared all cache entries");
            return true;
        } catch (Exception e) {
            log.error("Failed to clear all cache entries", e);
            return false;
        }
    }

    /**
     * Get cache statistics.
     */
    public CacheStatistics getStatistics() {
        try {
            Long totalKeys = redisTemplate.getConnectionFactory().getConnection().dbSize();
            return new CacheStatistics(totalKeys != null ? totalKeys : 0L);
        } catch (Exception e) {
            log.warn("Failed to get cache statistics", e);
            return new CacheStatistics(0L);
        }
    }

    /**
     * Cache statistics.
     */
    public static class CacheStatistics {
        private final long totalKeys;

        public CacheStatistics(long totalKeys) {
            this.totalKeys = totalKeys;
        }

        public long getTotalKeys() {
            return totalKeys;
        }
    }
}
