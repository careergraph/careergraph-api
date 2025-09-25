package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;

public interface AuthService {

    void register(AuthRequests.RegisterRequest request);

    void confirmOtp(AuthRequests.ConfirmOtpRequest request);

    void resendOtp(AuthRequests.ConfirmOtpRequest request);

    AuthResponses.TokenResponse login(AuthRequests.LoginRequest request);

    void logout(String accessToken);

    AuthResponses.TokenResponse refresh(String refreshToken);

    void forgotPassword(AuthRequests.ForgotPasswordRequest request);
    
    void resetPassword(AuthRequests.ResetPasswordRequest request);
}


