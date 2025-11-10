package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {

    void register(AuthRequests.RegisterRequest request, boolean isHR);

    String confirmOtp(AuthRequests.ConfirmOtpRequest request);

    Integer resendOtp(AuthRequests.ResendOtpRequest request);
    Long getTtlOtp(String email);

    AuthResponses.TokenResponse login(AuthRequests.LoginRequest request);

    void logout(String accessToken);

    AuthResponses.TokenResponse refresh(String refreshToken);

    Integer forgotPassword(AuthRequests.ForgotPasswordRequest request);
    
    void resetPassword(String resetPasswordToken, AuthRequests.ResetPasswordRequest request);

    AuthResponses.TokenResponse googleLogin(AuthRequests.GoogleLoginRequest request);

    AuthResponses.TokenResponse refreshWithFamily(Jwt jwt);

    String generateOTPToken(String email);
}


