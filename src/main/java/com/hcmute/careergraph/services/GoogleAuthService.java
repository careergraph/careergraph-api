package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.GoogleUserInfo;

public interface GoogleAuthService {
    
    /**
     * Verify Google ID token and extract user information.
     * 
     * @param idToken Google ID token from client
     * @return GoogleUserInfo containing user email and name
     * @throws com.hcmute.careergraph.exception.AppException if verification fails
     */
    GoogleUserInfo verify(String idToken);
}
