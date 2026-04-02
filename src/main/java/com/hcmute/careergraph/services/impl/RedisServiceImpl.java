package com.hcmute.careergraph.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.services.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise-grade Redis service for token management and caching.
 * Migrated from Tracking project with proper error handling and null-safety.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get object from Redis with proper error handling.
     * Handles String JSON, LinkedHashMap, and direct type casting.
     */
    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        if (key == null || key.isBlank()) {
            log.warn("Attempted to get object with null/blank key");
            return null;
        }

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }

            // Handle case where value is already the correct type
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }

            // Handle JSON string
            if (value instanceof String json) {
                return objectMapper.readValue(json, clazz);
            }

            // Handle LinkedHashMap or other Map types for complex objects
            return objectMapper.convertValue(value, clazz);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize object for key '{}': {}", key, e.getMessage());
            return null;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while getting key '{}': {}", key, e.getMessage());
            throw e; // Re-throw connection failures for circuit breaker handling
        } catch (Exception e) {
            log.error("Unexpected error getting object for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Set object in Redis with TTL.
     */
    @Override
    public <T> void setObject(String key, T value, Integer timeout) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        if (timeout == null || timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive, got: " + timeout);
        }

        try {
            // Value serializer is GenericJackson2JsonRedisSerializer, so store value as-is.
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout));
            log.debug("Set object for key '{}' with TTL {} seconds", key, timeout);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting key '{}': {}", key, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteObject(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while deleting key '{}': {}", key, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while checking key '{}': {}", key, e.getMessage());
            throw e;
        }
    }

    /**
     * Update field in stored JSON object.
     * Note: This method is NOT atomic.
     */
    @Override
    public void setField(String key, String field, Object value) {
        if (key == null || key.isBlank() || field == null || field.isBlank()) {
            throw new IllegalArgumentException("Key and field must not be null or blank");
        }

        try {
            Map<String, Object> meta = getObjectAsMap(key);
            if (meta == null) {
                meta = new HashMap<>();
            }

            meta.put(field, value);

            // Preserve existing TTL if available
            Long ttl = getTtl(key);
            // Default to 1 hour if no TTL found
            int effectiveTtl = (ttl == null || ttl < 0) ? 3600 : ttl.intValue();
            
            setObject(key, meta, effectiveTtl);
            log.debug("Updated field '{}' in key '{}' with TTL {} seconds", field, key, effectiveTtl);
            
        } catch (Exception e) {
            log.error("Failed to update field '{}' in key '{}': {}", field, key, e.getMessage());
            throw new RuntimeException("Failed to update field in Redis", e);
        }
    }

    /**
     * Get a specific field from a stored JSON object.
     * Fixed: Now correctly handles JSON string values instead of using opsForHash.
     */
    @Override
    public <T> T getField(String key, String field, Class<T> clazz) {
        if (key == null || key.isBlank() || field == null || field.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> data = getObjectAsMap(key);
            if (data == null || !data.containsKey(field)) {
                return null;
            }
            
            Object fieldValue = data.get(field);
            if (fieldValue == null) {
                return null;
            }
            
            return objectMapper.convertValue(fieldValue, clazz);
            
        } catch (Exception e) {
            log.error("Failed to get field '{}' from key '{}': {}", field, key, e.getMessage());
            return null;
        }
    }

    /**
     * Get object as Map for field operations.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getObjectAsMap(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
            
            if (value instanceof String json) {
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
            
            return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
            
        } catch (Exception e) {
            log.error("Failed to get object as map for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public Long getTtl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while getting TTL for key '{}': {}", key, e.getMessage());
            throw e;
        }
    }
}
