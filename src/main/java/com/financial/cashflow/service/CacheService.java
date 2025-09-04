package com.financial.cashflow.service;

import java.util.Optional;

/**
 * Service for caching operations.
 */
public interface CacheService {
    
    /**
     * Get a value from cache.
     * 
     * @param key The cache key
     * @param type The type to deserialize to
     * @param <T> The type parameter
     * @return Optional containing the value if found
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * Put a value in cache.
     * 
     * @param key The cache key
     * @param value The value to cache
     * @param <T> The type parameter
     * @return true if successful
     */
    <T> boolean put(String key, T value);
}
