package com.hcmute.careergraph.config.security;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class JwtConfig {

    @Value("${jwt.signer-key}")
    private String signerKey;

    private final RedisService redisService;
    private final AccountRepository accountRepository;
    private final UserCacheService userCacheService;

    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] keyBytes = Base64.getDecoder().decode(signerKey);
        return new SimpleJwtEncoder(signerKey);
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return new CustomJwtDecoder(signerKey, redisService, accountRepository, userCacheService);
    }

    /**
     * Raw decoder for internal use (logout, refresh token rotation).
     * Does NOT validate token type or blacklist - only verifies signature.
     * NEVER expose this to Spring Security filter chain.
     */
    @Bean("rawJwtDecoder")
    public JwtDecoder rawJwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(signerKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA384");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS384)
                .build();
    }

    // Custom exceptions for JWT validation errors
    public static class RoleChangedException extends BadJwtException {
        public RoleChangedException(String message) {
            super(message);
        }
    }

    public static class UserNotFoundException extends BadJwtException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class UserBlockedException extends BadJwtException {
        public UserBlockedException(String message) {
            super(message);
        }
    }

    public static class TokenBlacklistedException extends BadJwtException {
        public TokenBlacklistedException(String message) {
            super(message);
        }
    }

    public static class InvalidTokenSubjectException extends BadJwtException {
        public InvalidTokenSubjectException(String message) {
            super(message);
        }
    }

    // Custom JwtDecoder class with blacklist check + Redis-cached user lookup
    public static class CustomJwtDecoder implements JwtDecoder {

        private final RedisService redisService;
        private final NimbusJwtDecoder nimbusJwtDecoder;
        private final AccountRepository accountRepository;
        private final UserCacheService userCacheService;
        private static final String BLACKLIST_PREFIX = "bl-at:";

        public CustomJwtDecoder(String signerKey, RedisService redisService, AccountRepository accountRepository, UserCacheService userCacheService) {
            this.redisService = redisService;
            this.accountRepository = accountRepository;
            this.userCacheService = userCacheService;

            // Initialize decoder with HS384
            byte[] keyBytes = Base64.getDecoder().decode(signerKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA384");
            
            this.nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS384)
                    .build();
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            Jwt jwt = nimbusJwtDecoder.decode(token);
            String jti = jwt.getId();
            
            String type = jwt.getClaim("type");
            if (type == null || !type.equalsIgnoreCase("access")) {
                throw new JwtException("Invalid token type");
            }
            
            // Check blacklist
            Boolean blacklisted = redisService.getObject(BLACKLIST_PREFIX + jti, Boolean.class);
            if (blacklisted != null && blacklisted) {
                throw new TokenBlacklistedException("Token is blacklisted");
            }
            
            String accountId = jwt.getSubject();
            if (accountId == null) {
                throw new InvalidTokenSubjectException("Invalid token subject");
            }

            // Lookup user with Redis cache to avoid DB query on every request
            UserCacheService.UserCacheEntry cachedUser = getUserFromCache(accountId);
            if (cachedUser == null) {
                throw new UserNotFoundException("User not found");
            }
            if (!cachedUser.isActive()) {
                throw new UserBlockedException("User is blocked");
            }

            // Check if role has changed
            String tokenRole = jwt.getClaimAsString("role");
            if (!cachedUser.role().name().equals(tokenRole)) {
                // Blacklist this access token immediately
                blacklistToken(jwt, jti);
                // Invalidate cache so next request sees fresh data
                userCacheService.evict(accountId);
                throw new RoleChangedException(
                    "Your role has been changed from " + tokenRole + " to " + cachedUser.role() + 
                    ". Please login again to continue."
                );
            }

            return jwt;
        }

        /**
         * Get user from cache via UserCacheService, fallback to DB.
         */
        private UserCacheService.UserCacheEntry getUserFromCache(String accountId) {
            UserCacheService.UserCacheEntry cached = userCacheService.get(accountId);
            if (cached != null) {
                return cached;
            }

            // Cache miss — query DB
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return null;
            }

            userCacheService.put(accountId, account.getRole(), true);  // Assuming active if found
            return new UserCacheService.UserCacheEntry(account.getRole(), true);
        }

        /**
         * Blacklist the access token
         */
        private void blacklistToken(Jwt jwt, String jti) {
            try {
                Instant expiresAt = jwt.getExpiresAt();
                if (expiresAt != null) {
                    long ttl = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
                    if (ttl > 0) {
                        redisService.setObject(BLACKLIST_PREFIX + jti, true, (int) ttl);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to blacklist token {}: {}", jti, e.getMessage());
            }
        }
    }

    // Simple JWT Encoder (kept from original implementation)
    public static class SimpleJwtEncoder implements JwtEncoder {

        @Value("${jwt.valid-duration}")
        private long validDuration;

        @Value("${jwt.signer-key}")
        private String signerKey;

        public SimpleJwtEncoder(String signerKey) {
            this.signerKey = signerKey;
        }

        @Override
        public Jwt encode(JwtEncoderParameters parameters) throws JwtEncodingException {
            try {
                JwtClaimsSet claims = parameters.getClaims();
                
                // Create JWS header with HS384
                com.nimbusds.jose.JWSHeader jwsHeader = new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS384);
                
                // Build claims with all the information from the input claims
                com.nimbusds.jwt.JWTClaimsSet.Builder claimsBuilder = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                        .subject(claims.getSubject())
                        .issuer("careergraph-system")
                        .issueTime(claims.getIssuedAt() != null ? java.util.Date.from(claims.getIssuedAt()) : new java.util.Date())
                        .expirationTime(claims.getExpiresAt() != null ? java.util.Date.from(claims.getExpiresAt()) : new java.util.Date(System.currentTimeMillis() + 3600000))
                        .jwtID(claims.getId());
                
                // Add all custom claims - convert Instant to Date for compatibility
                for (java.util.Map.Entry<String, Object> entry : claims.getClaims().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Instant) {
                        claimsBuilder.claim(entry.getKey(), java.util.Date.from((Instant) value));
                    } else {
                        claimsBuilder.claim(entry.getKey(), value);
                    }
                }
                
                com.nimbusds.jwt.JWTClaimsSet jwtClaimsSet = claimsBuilder.build();
                
                // Create JWS object
                com.nimbusds.jose.Payload payload = new com.nimbusds.jose.Payload(jwtClaimsSet.toJSONObject());
                com.nimbusds.jose.JWSObject jwsObject = new com.nimbusds.jose.JWSObject(jwsHeader, payload);
                
                // Sign the JWT
                byte[] keyBytes = Base64.getDecoder().decode(signerKey);
                jwsObject.sign(new com.nimbusds.jose.crypto.MACSigner(keyBytes));
                
                String token = jwsObject.serialize();

                // Create headers map
                java.util.Map<String, Object> headers = new java.util.HashMap<>();
                headers.put("alg", jwsHeader.getAlgorithm().getName());
                headers.put("typ", "JWT");
                
                return new Jwt(token, 
                              claims.getIssuedAt(), 
                              claims.getExpiresAt(),
                              headers,
                              claims.getClaims());
            } catch (Exception e) {
                throw new JwtEncodingException("Failed to encode JWT: " + e.getMessage(), e);
            }
        }
    }
}