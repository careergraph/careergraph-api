package com.hcmute.careergraph.services;


import com.hcmute.careergraph.dtos.request.auth.IntrospectRequest;
import com.hcmute.careergraph.dtos.request.auth.LoginRequest;
import com.hcmute.careergraph.dtos.request.auth.LogoutRequest;
import com.hcmute.careergraph.dtos.request.auth.RefreshRequest;
import com.hcmute.careergraph.dtos.response.auth.IntrospectResponse;
import com.hcmute.careergraph.dtos.response.auth.LoginResponse;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.text.ParseException;

public interface IAuthService {

    IntrospectResponse introspect(IntrospectRequest request);

    LoginResponse login(LoginRequest request)
            throws ChangeSetPersister.NotFoundException;

    String refreshToken(RefreshRequest request)
            throws ParseException, ChangeSetPersister.NotFoundException;

    void logout(LogoutRequest request) throws ParseException;
}
