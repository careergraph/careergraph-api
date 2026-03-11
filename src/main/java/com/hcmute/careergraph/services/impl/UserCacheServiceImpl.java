package com.hcmute.careergraph.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.UserCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Redis-based user cache implementation.
 * Caches user role and active status for 5 minutes to reduce DB load.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserCacheServiceImpl implements UserCacheService {

    private final RedisService redisService;
    
    private static final String CACHE_KEY_PREFIX = "user:cache:";
    
    @Value("${cache.user.ttl:300}") // Default 5 minutes
    private int cacheTtl;

    @Override
    public UserCacheEntry get(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        
        String key = CACHE_KEY_PREFIX + accountId;
        Map<String, Object> data = redisService.getObject(key, Map.class);
        
        if (data == null) {
            return null;
        }
        
        try {
            Role role = Role.valueOf((String) data.get("role"));
            boolean isActive = (Boolean) data.get("isActive");
            return new UserCacheEntry(role, isActive);
        } catch (Exception e) {
            log.warn("Failed to parse user cache for accountId {}: {}", accountId, e.getMessage());
            return null;
        }
    }

    @Override
    public void put(String accountId, Role role, boolean isActive) {
        if (accountId == null || accountId.isBlank() || role == null) {
            return;
        }
        
        String key = CACHE_KEY_PREFIX + accountId;
        Map<String, Object> data = Map.of(
            "role", role.name(),
            "isActive", isActive
        );
        
        redisService.setObject(key, data, cacheTtl);
        log.debug("Cached user {} with role {} for {} seconds", accountId, role, cacheTtl);
    }

    @Override
    public void evict(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        
        String key = CACHE_KEY_PREFIX + accountId;
        redisService.deleteObject(key);
        log.debug("Evicted user cache for accountId {}", accountId);
    }
}
