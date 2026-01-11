package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GoogleUserInfo {
    private String email;
    private String name;
    private String picture;
    private String providerId;
}

