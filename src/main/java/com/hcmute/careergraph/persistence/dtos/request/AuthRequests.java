package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequests {

    @Data
    public static class RegisterRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        @Size(min = 8, max = 100)
        private String password;
    }

    @Data
    public static class ConfirmOtpRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String otp;
    }

    @Data
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class ForgotPasswordRequest {
        @Email
        @NotBlank
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String otp;
        @NotBlank
        @Size(min = 8, max = 100)
        private String newPassword;
    }

    @Data
    public static class GoogleLoginRequest {
        @NotBlank
        private String idToken;
    }
}


