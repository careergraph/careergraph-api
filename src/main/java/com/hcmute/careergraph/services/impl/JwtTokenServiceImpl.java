package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.IRedisService;
import com.hcmute.careergraph.services.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final IRedisService redisService;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.valid-duration}")
    private long accessTtl;

    @Value("${jwt.refreshable-duration}")
    private long refreshTtl;

    @Override
    public String generateAccessToken(Account account) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(account.getId())
                .claim("email", account.getEmail())
                .claim("role", account.getRole().name())
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(exp)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public String generateRefreshToken(Account account) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(account.getId())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(exp)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public boolean isBlacklisted(String jti) {
        Boolean value = redisService.getObject("bl:" + jti, Boolean.class);
        return value != null && value;
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        redisService.setObject("bl:" + jti, Boolean.TRUE, (int) ttlSeconds);
    }
}