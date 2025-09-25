package com.hcmute.careergraph.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.spec.SecretKeySpec;
import com.hcmute.careergraph.services.IRedisService;

import java.util.Base64;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @Value("${jwt.signer-key}")
    private String signerKey;

    private final IRedisService redisService;

    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] keyBytes = Base64.getDecoder().decode(signerKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
        
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return new CustomJwtDecoder(signerKey, redisService);
    }

    // Custom JwtDecoder class để handle blacklist
    public static class CustomJwtDecoder implements JwtDecoder {

        private final String signerKey;
        private final IRedisService redisService;
        private NimbusJwtDecoder nimbusJwtDecoder;

        public CustomJwtDecoder(String signerKey, IRedisService redisService) {
            this.signerKey = signerKey;
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
}