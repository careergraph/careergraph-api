package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.services.AuthService;
import com.hcmute.careergraph.services.RedisService;
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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtTokenService jwtTokenService;
    private final RedisService redisService;
    private final MailService mailService;
    private final JwtDecoder jwtDecoder;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Value("${jwt.valid-duration}")
    private long accessTtl;

    @Value("${jwt.refreshable-duration}")
    private long refreshTtl;

    @Override
    public void register(AuthRequests.RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorType.CONFLICT, "Invalid OTP");
        }

        // Create account
        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .build();

        // Create candidate
        Candidate candidate = Candidate.builder()
                .account(account)
                .build();
        account.setCandidate(candidate);

        candidateRepository.save(candidate);

        String otp = generateOtp();
        redisService.setObject(otpKey(request.getEmail()), otp, 300);
        mailService.sendOtp(request.getEmail(), otp);
    }

    @Override
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

    @Override
    public AuthResponses.TokenResponse googleLogin(AuthRequests.GoogleLoginRequest request) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new AppException(ErrorType.UNAUTHORIZED, "Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String givenName = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");
            String pictureUrl = (String) payload.get("picture");

            Account account = accountRepository.findByEmail(email).orElse(null);
            if (account == null) {
                // Create new account and candidate
                Candidate candidate = Candidate.builder().build();
                account = Account.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("google:" + idToken.getPayload().getSubject()))
                        .role(Role.USER)
                        .emailVerified(emailVerified)
                        .candidate(candidate)
                        .build();
                candidate.setAccount(account);
                // Attach basic profile
                candidate.setFirstName(givenName);
                candidate.setLastName(familyName);
                candidate.setAvatar(pictureUrl);
                candidateRepository.save(candidate);
            } else {
                // Existing account: mark verified if Google confirmed
                if (emailVerified && !account.isEmailVerified()) {
                    account.setEmailVerified(true);
                    accountRepository.save(account);
                }
                // Ensure candidate exists and update profile lightly
                Candidate candidate = account.getCandidate();
                if (candidate == null) {
                    candidate = Candidate.builder()
                            .account(account)
                            .build();
                    account.setCandidate(candidate);
                }
                if (candidate.getAvatar() == null && pictureUrl != null) {
                    candidate.setAvatar(pictureUrl);
                }
                if (candidate.getFirstName() == null && givenName != null) {
                    candidate.setFirstName(givenName);
                }
                if (candidate.getLastName() == null && familyName != null) {
                    candidate.setLastName(familyName);
                }
                candidateRepository.save(candidate);
            }

            String accessToken = jwtTokenService.generateAccessToken(account);
            String refreshToken = jwtTokenService.generateRefreshToken(account);
            return AuthResponses.TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(accessTtl)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Failed to verify Google token");
        }
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


