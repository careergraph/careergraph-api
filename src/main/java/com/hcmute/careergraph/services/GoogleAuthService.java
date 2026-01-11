package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.GoogleUserInfo;

public interface GoogleAuthService {
    GoogleUserInfo verify(String idTokenString);
}
