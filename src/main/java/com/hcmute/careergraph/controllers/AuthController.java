package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RestResponse<Void> register(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        authService.register(request);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Registered Successfully")
                .build();
    }

    @PostMapping("/confirm-otp")
    public RestResponse<Void> confirmOtp(@Valid @RequestBody AuthRequests.ConfirmOtpRequest request) {
        authService.confirmOtp(request);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("OTP confirmed successfully")
                .build();
    }

    @PostMapping("/resend-otp")
    public RestResponse<Void> resendOtp(@Valid @RequestBody AuthRequests.ConfirmOtpRequest request) {
        authService.resendOtp(request);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("OTP resent successfully")
                .build();
    }

    @PostMapping("/login")
    public RestResponse<AuthResponses.TokenResponse> login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        var tokens = authService.login(request);
        return RestResponse.<AuthResponses.TokenResponse>builder()
                .status(HttpStatus.OK)
                .message("Login successful")
                .data(tokens)
                .build();
    }

    @PostMapping("/logout")
    public RestResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        authService.logout(token);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Logout successful")
                .build();
    }

    @PostMapping("/refresh-token")
    public RestResponse<AuthResponses.TokenResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        var tokens = authService.refresh(refreshToken);
        return RestResponse.<AuthResponses.TokenResponse>builder()
                .status(HttpStatus.OK)
                .message("Token refreshed successfully")
                .data(tokens)
                .build();
    }

    @PostMapping("/forgot-password")
    public RestResponse<Void> forgotPassword(@Valid @RequestBody AuthRequests.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Password reset link sent successfully")
                .build();
    }

    @PostMapping("/reset-password")
    public RestResponse<Void> resetPassword(@Valid @RequestBody AuthRequests.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Password reset successfully")
                .build();
    }

    @PostMapping("/google-login")
    public RestResponse<AuthResponses.TokenResponse> googleLogin(@Valid @RequestBody AuthRequests.GoogleLoginRequest request) {
        var tokens = authService.googleLogin(request);
        return RestResponse.<AuthResponses.TokenResponse>builder()
                .status(HttpStatus.OK)
                .message("Login successful")
                .data(tokens)
                .build();
    }
}
