package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Account;

public interface JwtTokenService {

    String generateAccessToken(Account account);

    String generateRefreshToken(Account account);

    boolean isBlacklisted(String jti);
    
    void blacklist(String jti, long ttlSeconds);

    String rotateRefreshToken(Account account, String familyId);

    String generateRefreshTokenWithFamily(Account account);
}


