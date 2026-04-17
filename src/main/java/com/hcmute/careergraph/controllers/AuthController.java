package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.schedule.DailyDigestScheduler;
import com.hcmute.careergraph.services.AuthService;
import com.hcmute.careergraph.services.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class AuthController {

        private final AuthService authService;
        private final JwtDecoder rawJwtDecoder;
        private final AuthServiceImpl authServiceImpl;
        private final DailyDigestScheduler dailyDigestScheduler;
        private final SecurityUtils securityUtils;

        @Value("${cookie.secure:false}")
        private boolean cookieSecure;
        @Value("${cookie.samesite:Lax}")
        private String cookieSameSite;
        @Value("${jwt.refreshable-duration}")
        private long refreshTtl;

        private static final String REFRESH_COOKIE = "rt";
        private static final String RESET_PASSWORD_COOKIE = "ttl_t";

        public AuthController(AuthService authService,
                        @Qualifier("rawJwtDecoder") JwtDecoder rawJwtDecoder,
                        AuthServiceImpl authServiceImpl,
                        DailyDigestScheduler dailyDigestScheduler,
                        SecurityUtils securityUtils) {
                this.authService = authService;
                this.rawJwtDecoder = rawJwtDecoder;
                this.authServiceImpl = authServiceImpl;
                this.dailyDigestScheduler = dailyDigestScheduler;
                this.securityUtils = securityUtils;
        }

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

        @PostMapping("/confirm-otp-register")
        public RestResponse<Void> confirmOtpRegister(@Valid @RequestBody AuthRequests.ConfirmOtpRequest request) {
                authService.confirmRegisterOtp(request);
                return RestResponse.<Void>builder()
                                .status(HttpStatus.OK)
                                .message("OTP confirmed successfully")
                                .build();
        }

        @PostMapping("/confirm-otp-reset-password")
        public RestResponse<Void> confirmOtpResetPassWord(@Valid @RequestBody AuthRequests.ConfirmOtpRequest request,
                        HttpServletResponse resp) {
                String restPasswordToken = authService.confirmResetPasswordOtp(request);

                long resetPasswordTtl = 1000L;
                ResponseCookie cookie = ResponseCookie.from(RESET_PASSWORD_COOKIE, restPasswordToken)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .sameSite(cookieSameSite)
                                .path("/")
                                .maxAge(Duration.ofSeconds(resetPasswordTtl))
                                .build();
                resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                return RestResponse.<Void>builder()
                                .status(HttpStatus.OK)
                                .message("OTP confirmed successfully")
                                .build();
        }

        @PostMapping("/resend-otp")
        public RestResponse<Integer> resendOtp(@Valid @RequestBody AuthRequests.ResendOtpRequest request) {
                return RestResponse.<Integer>builder()
                                .status(HttpStatus.OK)
                                .data(authService.resendOtp(request))
                                .message("OTP resent successfully")
                                .build();
        }

        @GetMapping("/ttl-otp")
        public RestResponse<Long> getTtlOtp(@RequestParam("email") @Email @NotBlank String email) {
                return RestResponse.<Long>builder()
                                .status(HttpStatus.OK)
                                .data(authService.getTtlOtp(email))
                                .message("OTP resent successfully")
                                .build();
        }

        @PostMapping("/login")
        public RestResponse<AuthResponses.OnlyTokenResponse> login(
                        @Valid @RequestBody AuthRequests.LoginRequest request, HttpServletResponse resp) {
                dailyDigestScheduler.sendDailyDigest();
                var tokens = authService.login(request);
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
                                .data(AuthResponses.OnlyTokenResponse.builder().accessToken(tokens.getAccessToken())
                                                .build())
                                .build();
        }

        @PostMapping("/logout")
        public RestResponse<Void> logout(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        HttpServletResponse resp,
                        @CookieValue(name = REFRESH_COOKIE, required = false) String refreshCookie) {

                String token = authHeader != null && authHeader.startsWith("Bearer ")
                                ? authHeader.substring(7)
                                : authHeader;
                authService.logout(token);

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
        public RestResponse<Integer> forgotPassword(@Valid @RequestBody AuthRequests.ForgotPasswordRequest request) {
                Integer ttl = authService.forgotPassword(request);
                return RestResponse.<Integer>builder()
                                .status(HttpStatus.OK)
                                .data(ttl)
                                .message("Password reset link sent successfully")
                                .build();
        }

        @PutMapping("/reset-password")
        public RestResponse<Void> resetPassword(
                        @CookieValue(name = RESET_PASSWORD_COOKIE, required = false) String resetPasswordCookie,
                        HttpServletRequest request, HttpServletResponse response,
                        @Valid @RequestBody AuthRequests.ResetPasswordRequest resetRequest) {

                if (resetPasswordCookie == null || resetPasswordCookie.isBlank()) {
                        return RestResponse.<Void>builder()
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .message("Missing reset password token")
                                        .build();
                }
                authService.resetPassword(resetPasswordCookie, resetRequest);
                ResponseCookie expiredCookie = ResponseCookie.from(RESET_PASSWORD_COOKIE, "")
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .sameSite(cookieSameSite)
                                .path("/")
                                .maxAge(Duration.ZERO)
                                .build();
                response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

                return RestResponse.<Void>builder()
                                .status(HttpStatus.OK)
                                .message("Password reset successfully")
                                .build();
        }

        @PostMapping("/email-change/request-otp")
        public RestResponse<Integer> requestEmailChangeOtp(@Valid @RequestBody AuthRequests.RequestEmailChangeOtp request) {
                Integer ttl = authService.requestEmailChangeOtp(getCurrentAccountId(), request);
                return RestResponse.<Integer>builder()
                                .status(HttpStatus.OK)
                                .message("OTP sent successfully")
                                .data(ttl)
                                .build();
        }

        @PostMapping("/email-change/confirm")
        public RestResponse<Void> confirmEmailChange(@Valid @RequestBody AuthRequests.ConfirmEmailChangeRequest request) {
                authService.confirmEmailChange(getCurrentAccountId(), request);
                return RestResponse.<Void>builder()
                                .status(HttpStatus.OK)
                                .message("Email updated successfully")
                                .build();
        }

        @PostMapping("/password-change/request-otp")
        public RestResponse<Integer> requestPasswordChangeOtp(@Valid @RequestBody AuthRequests.RequestPasswordChangeOtp request) {
                Integer ttl = authService.requestPasswordChangeOtp(getCurrentAccountId(), request);
                return RestResponse.<Integer>builder()
                                .status(HttpStatus.OK)
                                .message("OTP sent successfully")
                                .data(ttl)
                                .build();
        }

        @PostMapping("/password-change/confirm")
        public RestResponse<Void> confirmPasswordChange(@Valid @RequestBody AuthRequests.ConfirmPasswordChangeRequest request) {
                authService.confirmPasswordChange(getCurrentAccountId(), request);
                return RestResponse.<Void>builder()
                                .status(HttpStatus.OK)
                                .message("Password updated successfully")
                                .build();
        }

        @PostMapping("/google-login")
        public RestResponse<AuthResponses.OnlyTokenResponse> googleLogin(
                        @Valid @RequestBody AuthRequests.GoogleLoginRequest request, HttpServletResponse resp) {
                var tokens = authService.googleLogin(request);
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
                                .data(AuthResponses.OnlyTokenResponse.builder().accessToken(tokens.getAccessToken())
                                                .build())
                                .build();
        }

        @PostMapping("/refresh")
        public RestResponse<AuthResponses.OnlyTokenResponse> refresh(
                        @CookieValue(name = REFRESH_COOKIE, required = false) String refreshCookie,
                        HttpServletRequest request, HttpServletResponse response) {
                if (refreshCookie == null || refreshCookie.isBlank()) {
                        return RestResponse.<AuthResponses.OnlyTokenResponse>builder()
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .message("Missing refresh cookie")
                                        .build();
                }
                Jwt jwt = rawJwtDecoder.decode(refreshCookie);
                if (!jwt.getClaim("type").equals("refresh")) {
                        return RestResponse.<AuthResponses.OnlyTokenResponse>builder().status(HttpStatus.UNAUTHORIZED)
                                        .message("Invalid refresh cookie").build();
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
                                .data(AuthResponses.OnlyTokenResponse.builder().accessToken(token.getAccessToken())
                                                .build())
                                .build();
        }

        private String getCurrentAccountId() {
                return securityUtils.getCurrentAccount()
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"))
                                .getId();
        }

}