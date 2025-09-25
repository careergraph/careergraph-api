package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponses {

    @Data
    @Builder
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
    }
}


