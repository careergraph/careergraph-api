package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Account;

public interface JwtTokenService {

    String generateAccessToken(Account account);

    String generateRefreshToken(Account account);

    boolean isBlacklisted(String jti);
    
    void blacklist(String jti, long ttlSeconds);
}


