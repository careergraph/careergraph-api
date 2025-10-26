package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.services.AuthService;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RedisService redisService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${cookie.samesite:Lax}")
    private String cookieSameSite;

    private static final String REFRESH_COOKIE = "rt";
    private final JwtDecoder jwtDecoder;
    private final AuthServiceImpl authServiceImpl;

    @Value("${jwt.refreshable-duration}")
    private long refreshTtl;

    @PostMapping("/register/candidate")
    public RestResponse<Void> registerForCandidate(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        authService.register(request, false);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Registered Successfully")
                .build();
    }

    @PostMapping("/register/hr")
    public RestResponse<Void> registerForHR(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        authService.register(request, true);
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
    public RestResponse<AuthResponses.OnlyTokenResponse> login(@Valid @RequestBody AuthRequests.LoginRequest request, HttpServletResponse resp) {
        var tokens = authService.login(request);
        System.out.println("tokens: " + tokens);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, tokens.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTtl))
                .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return RestResponse.<AuthResponses.OnlyTokenResponse>builder()
                .status(HttpStatus.OK)
                .message("Login successful")
                .data(AuthResponses.OnlyTokenResponse.builder().accessToken(tokens.getAccessToken()).build())
                .build();
    }

    @PostMapping("/logout")
    public RestResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse resp,
            @CookieValue(name =  REFRESH_COOKIE, required = false) String refreshCookie) {

        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        authService.logout(token);

        System.out.println("token: " + token);
        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
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
    @PostMapping("/refresh")
    public RestResponse<AuthResponses.OnlyTokenResponse> refresh(@CookieValue(name= REFRESH_COOKIE, required = false) String refreshCookie,
                                   HttpServletRequest request, HttpServletResponse response) {
        System.out.println("vao refresh: " + refreshCookie);
        if(refreshCookie == null || refreshCookie.isBlank()) {
            return RestResponse.<AuthResponses.OnlyTokenResponse>builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Missing refresh cookie")
                    .build();
        }
        Jwt jwt =  jwtDecoder.decode(refreshCookie);
        if(!jwt.getClaim("type").equals("refresh")) {
            return RestResponse.<AuthResponses.OnlyTokenResponse>builder().status(HttpStatus.UNAUTHORIZED).message("Invalid refresh cookie").build();
        }

        AuthResponses.TokenResponse token = authServiceImpl.refreshWithFamily(jwt);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, token.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTtl))
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return RestResponse.<AuthResponses.OnlyTokenResponse>builder()
                .status(HttpStatus.OK)
                .data(AuthResponses.OnlyTokenResponse.builder().accessToken(token.getAccessToken()).build())
                .build();
    }

}
