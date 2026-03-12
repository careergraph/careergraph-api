package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final RedisService redisService;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.valid-duration}")
    private long accessTtl;

    @Value("${jwt.refreshable-duration}")
    private Integer refreshTtl;
    
    // Minimum TTL to prevent issues with near-expired tokens
    private static final long MIN_TTL_SECONDS = 10;

    @Override
    public String generateAccessToken(Account account) {
        return generateAccessToken(account, null);
    }
    
    @Override
    public String generateAccessToken(Account account, String familyId) {
        if (account == null || account.getId() == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtl);
        String jti = UUID.randomUUID().toString();

        var candidateName = "";
        if (account.getCandidate() != null) {
            var c = account.getCandidate();
            var first = c.getFirstName() != null ? c.getFirstName() : "";
            var last = c.getLastName() != null ? c.getLastName() : "";
            candidateName = (first + " " + last).trim();
        }

        var builder = JwtClaimsSet.builder()
                .subject(account.getId())
                .claim("email", account.getEmail())
                .claim("role", account.getRole().name())
                .claim("candidateId", account.getCandidate() != null ? account.getCandidate().getId() : "")
                .claim("companyId", account.getCompany() != null ? account.getCompany().getId() : "")
                .claim("fullName", candidateName)
                .claim("type", "access")
                .id(jti)
                .issuedAt(now)
                .expiresAt(exp);

        if (familyId != null) {
            builder.claim("fam", familyId);
        }

        log.debug("Generated access token for account {} with jti {}", account.getId(), jti);
        return jwtEncoder.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
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
    public String generateRefreshTokenWithFamily(Account account) {
        Instant familyExp = Instant.now().plusSeconds(refreshTtl);
        String familyId = UUID.randomUUID().toString();
        redisService.setObject("rt:fam:exp:" + familyId, familyExp.getEpochSecond(), refreshTtl);
        return generateRefreshTokenWithFamily(account, familyId);
    }

    @Override
    public String generateResetPasswordToken(String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(300);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .claim("type", "opt_token")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(exp)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public String rotateRefreshToken(Account account, String familyId){
        return generateRefreshTokenWithFamily(account, familyId);
    }
    
    private String generateRefreshTokenWithFamily(Account account, String familyId){
        if (account == null || account.getId() == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("FamilyId must not be null or blank");
        }
        
        Instant now = Instant.now();
        Long familyExp = redisService.getObject("rt:fam:exp:" + familyId, Long.class);
        if (familyExp == null) {
            log.warn("Refresh family {} has expired or does not exist", familyId);
            throw new AppException(ErrorType.UNAUTHORIZED, "Refresh family expired");
        }
        
        Instant exp = Instant.ofEpochSecond(familyExp);
        String jti = UUID.randomUUID().toString();
        
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .claim("type", "refresh")
                .claim("fam", familyId)
                .claim("email", account.getEmail())
                .claim("role", account.getRole().name())  // Include role for change detection
                .subject(account.getId())
                .id(jti)
                .issuedAt(now)
                .expiresAt(exp)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        // Calculate TTL safely - ensure it's positive
        long ttl = Math.max(familyExp - Instant.now().getEpochSecond(), MIN_TTL_SECONDS);

        // Store in Redis: key by jti, TTL = remaining family time
        redisService.setObject("rt:jti:" + jti, Map.of(
                "accountId", account.getId(),
                "fam", familyId,
                "exp", exp.getEpochSecond(),
                "revoked", false
        ), (int) ttl);

        // Also store current jti by family for quick family revocation on reuse
        redisService.setObject("rt:fam:current:" + familyId, jti, refreshTtl);
        
        log.debug("Generated refresh token for account {} with jti {} in family {}", 
                account.getId(), jti, familyId);

        return token;
    }


    @Override
    public boolean isBlacklisted(String jti) {
        Boolean value = redisService.getObject("bl-at:" + jti, Boolean.class);
        return value != null && value;
    }

    @Override
    public void blacklist(String keyPrefix, String jti, long ttlSeconds) {
        redisService.setObject(keyPrefix + ":" + jti, Boolean.TRUE, (int) ttlSeconds);
    }
}