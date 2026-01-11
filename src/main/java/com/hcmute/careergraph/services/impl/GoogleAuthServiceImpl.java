package com.hcmute.careergraph.services.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.hcmute.careergraph.persistence.dtos.response.GoogleUserInfo;
import com.hcmute.careergraph.services.GoogleAuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthServiceImpl() {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new JacksonFactory()
        ).build();
    }
    @Override
    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Check audience
            if (!payload.getAudience().equals(clientId)) {
                throw new RuntimeException("Invalid audience");
            }

            // Check email verified
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                throw new RuntimeException("Email not verified");
            }

            return GoogleUserInfo.builder()
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .providerId(payload.getSubject())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Google token verification failed", e);
        }

    }
}
