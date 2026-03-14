package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.Role;

/**
 * User cache service for high-performance user data access.
 * Caches role and active status to avoid DB queries on every request.
 */
public interface UserCacheService {
    
    /**
     * Cache entry containing user role and active status.
     */
    record UserCacheEntry(Role role, boolean isActive) {}
    
    /**
     * Get user cache entry.
     * 
     * @param accountId Account ID
     * @return UserCacheEntry or null if not cached
     */
    UserCacheEntry get(String accountId);
    
    /**
     * Put user data into cache.
     * 
     * @param accountId Account ID
     * @param role User role
     * @param isActive Whether user is active
     */
    void put(String accountId, Role role, boolean isActive);
    
    /**
     * Evict user from cache (e.g., when role changes).
     * 
     * @param accountId Account ID
     */
    void evict(String accountId);
}
