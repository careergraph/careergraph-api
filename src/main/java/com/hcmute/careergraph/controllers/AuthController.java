package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RestResponse<Void>> register(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(RestResponse.<Void>builder().build());
    }

    @PostMapping("/confirm-otp")
    public ResponseEntity<RestResponse<Void>> confirmOtp(@Valid @RequestBody AuthRequests.ConfirmOtpRequest request) {
        authService.confirmOtp(request);
        return ResponseEntity.ok(RestResponse.<Void>builder().build());
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<RestResponse<Void>> resendOtp(@Valid @RequestBody AuthRequests.ConfirmOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(RestResponse.<Void>builder().build());
    }

    @PostMapping("/login")
    public ResponseEntity<RestResponse<AuthResponses.TokenResponse>> login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        var tokens = authService.login(request);
        return ResponseEntity.ok(RestResponse.<AuthResponses.TokenResponse>builder().data(tokens).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<RestResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        authService.logout(token);
        return ResponseEntity.ok(RestResponse.<Void>builder().build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RestResponse<AuthResponses.TokenResponse>> refresh(@RequestParam("refreshToken") String refreshToken) {
        var tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(RestResponse.<AuthResponses.TokenResponse>builder().data(tokens).build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<RestResponse<Void>> forgotPassword(@Valid @RequestBody AuthRequests.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(RestResponse.<Void>builder().build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<RestResponse<Void>> resetPassword(@Valid @RequestBody AuthRequests.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(RestResponse.<Void>builder().build());
    }
}


