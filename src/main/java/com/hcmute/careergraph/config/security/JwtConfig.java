package com.hcmute.careergraph.config.security;

import com.hcmute.careergraph.services.RedisService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @Value("${jwt.signer-key}")
    private String signerKey;

    private final RedisService redisService;

    @Bean
    public JwtEncoder jwtEncoder() {
        return new SimpleJwtEncoder(signerKey);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return new CustomJwtDecoder(signerKey, redisService);
    }

    // Custom JwtDecoder class để handle blacklist
    public static class CustomJwtDecoder implements JwtDecoder {

        private final RedisService redisService;
        private final NimbusJwtDecoder nimbusJwtDecoder;

        public CustomJwtDecoder(String signerKey, RedisService redisService) {
            this.redisService = redisService;

            // Initialize decoder
            byte[] keyBytes = Base64.getDecoder().decode(signerKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA512");
            
            this.nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            Jwt jwt = nimbusJwtDecoder.decode(token);
            String jti = jwt.getId();

            // Check blacklist
            Boolean blacklisted = redisService.getObject("bl:" + jti, Boolean.class);
            if (blacklisted != null && blacklisted) {
                throw new JwtException("Token is blacklisted");
            }

            return jwt;
        }
    }

    // Simple JWT Encoder
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
                
                // Create JWS header
                JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
                
                // Build claims with all the information from the input claims
                JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                        .subject(claims.getSubject())
                        .issuer("careergraph-system")
                        .issueTime(claims.getIssuedAt() != null ? Date.from(claims.getIssuedAt()) : new Date())
                        .expirationTime(claims.getExpiresAt() != null ? Date.from(claims.getExpiresAt()) : new Date(System.currentTimeMillis() + 3600000))
                        .jwtID(claims.getId());
                
                // Add all custom claims - convert Instant to Date for compatibility
                for (Map.Entry<String, Object> entry : claims.getClaims().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Instant) {
                        claimsBuilder.claim(entry.getKey(), Date.from((Instant) value));
                    } else {
                        claimsBuilder.claim(entry.getKey(), value);
                    }
                }
                
                JWTClaimsSet jwtClaimsSet = claimsBuilder.build();
                
                // Create JWS object
                Payload payload = new Payload(jwtClaimsSet.toJSONObject());
                JWSObject jwsObject = new JWSObject(jwsHeader, payload);
                
                // Sign the JWT
                byte[] keyBytes = Base64.getDecoder().decode(signerKey);
                jwsObject.sign(new MACSigner(keyBytes));
                
                String token = jwsObject.serialize();

                // Create headers map
                Map<String, Object> headers = new HashMap<>();
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