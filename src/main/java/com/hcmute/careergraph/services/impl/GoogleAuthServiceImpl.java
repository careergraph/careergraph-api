package com.hcmute.careergraph.services.impl;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.persistence.dtos.response.GoogleUserInfo;
import com.hcmute.careergraph.services.GoogleAuthService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth ID Token Verification Service.
 * Verifies Google ID tokens from client-side OAuth flow.
 */
@Service
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        this.verifier = new GoogleIdTokenVerifier
                .Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        GoogleIdToken token;
        try {
            // Parse without verification first to debug audience
            GoogleIdToken parsed = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), idToken);
            if (parsed != null) {
                log.info("Google ID token audience: {}, expected: {}", parsed.getPayload().getAudience(), clientId);
                log.info("Google ID token issuer: {}, email: {}", parsed.getPayload().getIssuer(), parsed.getPayload().getEmail());
            }
            token = verifier.verify(idToken);
        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw new AppException(ErrorType.UNAUTHORIZED, "Google authentication failed");
        }

        if (token == null) {
            log.warn("Google token verification returned null (invalid signature or audience mismatch)");
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid Google token");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        return GoogleUserInfo.builder()
                .email(payload.getEmail())
                .emailVerified(Boolean.TRUE.equals(payload.getEmailVerified()))
                .name((String)payload.get("name"))
                .givenName((String) payload.get("given_name"))
                .familyName((String) payload.get("family_name"))
                .picture((String) payload.get("picture"))
                .build();
    }
}
