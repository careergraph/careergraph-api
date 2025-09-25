package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.ErrorType;
import com.hcmute.careergraph.enums.Role;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.services.AuthService;
import com.hcmute.careergraph.services.IRedisService;
import com.hcmute.careergraph.services.JwtTokenService;
import com.hcmute.careergraph.services.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final IRedisService redisService;
    private final MailService mailService;
    private final JwtDecoder jwtDecoder;

    @Value("${jwt.valid-duration}")
    private long accessTtl;

    @Value("${jwt.refreshable-duration}")
    private long refreshTtl;

    @Override
    @Transactional
    public void register(AuthRequests.RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorType.CONFLICT, "Invalid OTP");
        }
        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .build();
        accountRepository.save(account);

        String otp = generateOtp();
        redisService.setObject(otpKey(request.getEmail()), otp, 300);
        mailService.sendOtp(request.getEmail(), otp);
    }

    @Override
    @Transactional
    public void confirmOtp(AuthRequests.ConfirmOtpRequest request) {
        String cachedOtp = redisService.getObject(otpKey(request.getEmail()), String.class);

        if (cachedOtp == null) {
            throw new AppException(ErrorType.BAD_REQUEST, "OTP expired or not found");
        }
        if (!cachedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Invalid OTP");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        account.setEmailVerified(true);
        accountRepository.save(account);

        redisService.deleteObject(otpKey(request.getEmail()));
    }

    @Override
    public void resendOtp(AuthRequests.ConfirmOtpRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        if (account.isEmailVerified()) {
            throw new AppException(ErrorType.BAD_REQUEST, "Email already verified");
        }

        String otp = generateOtp();

        redisService.setObject(otpKey(request.getEmail()), otp, 300);

        mailService.sendOtp(request.getEmail(), otp);
    }


    @Override
    public AuthResponses.TokenResponse login(AuthRequests.LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid password");
        }
        String accessToken = jwtTokenService.generateAccessToken(account);
        String refreshToken = jwtTokenService.generateRefreshToken(account);
        return AuthResponses.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    @Override
    public void logout(String accessToken) {
        try {
            // Decode the access token to get JTI
            Jwt jwt = jwtDecoder.decode(accessToken);
            String jti = jwt.getId();
            
            // Blacklist the token using JTI
            jwtTokenService.blacklist(jti, accessTtl);
        } catch (JwtException e) {
            // If token is invalid, we can't blacklist it, but that's okay
            // The token will naturally expire
        }
    }

    @Override
    public AuthResponses.TokenResponse refresh(String refreshToken) {
        try {
            // Decode the refresh token to get claims
            Jwt jwt = jwtDecoder.decode(refreshToken);
            
            // Validate token type
            String tokenType = jwt.getClaimAsString("type");
            if (!"refresh".equals(tokenType)) {
                throw new AppException(ErrorType.UNAUTHORIZED, "Invalid token type");
            }
            
            // Check if token is blacklisted
            String jti = jwt.getId();
            if (jwtTokenService.isBlacklisted(jti)) {
                throw new AppException(ErrorType.UNAUTHORIZED, "Token is blacklisted");
            }
            
            // Get account from subject (account ID)
            String accountId = jwt.getSubject();
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));
            
            // Generate new tokens
            String newAccessToken = jwtTokenService.generateAccessToken(account);
            String newRefreshToken = jwtTokenService.generateRefreshToken(account);
            
            // Blacklist the old refresh token
            jwtTokenService.blacklist(jti, refreshTtl);
            
            return AuthResponses.TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(accessTtl)
                    .build();
                    
        } catch (JwtException e) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    @Override
    public void forgotPassword(AuthRequests.ForgotPasswordRequest request) {
        Optional<Account> accountOpt = accountRepository.findByEmail(request.getEmail());
        if (accountOpt.isEmpty()) return; // do not reveal
        String otp = generateOtp();
        redisService.setObject(otpKey(request.getEmail()), otp, 300);
        mailService.sendOtp(request.getEmail(), otp);
    }

    @Override
    @Transactional
    public void resetPassword(AuthRequests.ResetPasswordRequest request) {
        String cached = redisService.getObject(otpKey(request.getEmail()), String.class);
        if (cached == null || !cached.equals(request.getOtp())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Invalid OTP");
        }
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Invalid OTP"));
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        redisService.deleteObject(otpKey(request.getEmail()));
    }

    /*============================================ HELPER METHOD ============================================*/

    private String otpKey(String email) {
        return "otp:" + email;
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int value = 100000 + random.nextInt(900000);
        return String.valueOf(value);
    }
}


