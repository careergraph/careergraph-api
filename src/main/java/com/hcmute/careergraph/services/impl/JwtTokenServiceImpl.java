package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.JwtTokenService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final RedisService redisService;
    private final JwtEncoder jwtEncoder;

//    @Value("${jwt.issuer}")
//    private String issuer;
//    @Value("${jwt.audience}")
//    private String audience;

    @Value("${jwt.valid-duration}")
    private long accessTtl;

    @Value("${jwt.refreshable-duration}")
    private Integer refreshTtl;

    @Override
    public String generateAccessToken(Account account) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(account.getId())
                .claim("email", account.getEmail())
                .claim("role", account.getRole().name())
                .claim("candidateId", account.getCandidate() != null ? account.getCandidate().getId() : "")
                .claim("companyId", account.getCompany() != null ? account.getCompany().getId() : "")
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
    public String generateRefreshTokenWithFamily(Account account) {
        return generateRefreshTokenWithFamily(account,UUID.randomUUID().toString());
    }
    @Override
    public String rotateRefreshToken(Account account, String familyId){
        return generateRefreshTokenWithFamily(account, familyId);
    }
    private String generateRefreshTokenWithFamily(Account account, String familyId){
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtl);
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
//                .issuer(issuer)
//                .audience(List.copyOf(audience))
                .claim("type", "refresh")
                .claim("fam", familyId)
                .subject(account.getId())
                .id(jti)
                .issuedAt(now)
                .expiresAt(exp)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        //Lưu vào Redis: key theo jti, TTL = refreshTtl
        //value có thể là JSON( userId, fam, exp, revoked: false)
        redisService.setObject("rt:jti:" + jti, Map.of(
                "accountId", account.getId(),
                "fam", familyId,
                "exp", exp.getEpochSecond(),
                "revoked", false
        ), refreshTtl);

        // Cũng lưu current jti theo family để nhanh revoke family khi reuse
        redisService.setObject("rt:family:current:" + familyId, jti, refreshTtl);

        return token;
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