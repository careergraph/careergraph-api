package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

@Builder
public record GoogleUserInfo(
        String email,
        Boolean emailVerified,
        String name,
        String givenName,
        String familyName,
        String picture) {
}
