package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.dtos.response.AuthResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.persistence.dtos.response.GoogleUserInfo;
import com.hcmute.careergraph.services.AuthService;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.GoogleAuthService;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.JwtTokenService;
import com.hcmute.careergraph.services.MailService;
import com.hcmute.careergraph.services.UserCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final CandidateRepository candidateRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtTokenService jwtTokenService;
    private final RedisService redisService;
    private final MailService mailService;
    private final JwtDecoder jwtDecoder; // Primary decoder for validation
    private final JwtDecoder rawJwtDecoder; // Raw decoder for internal operations
    private final GoogleAuthService googleAuthService;
    private final UserCacheService userCacheService;
    private final CompanyRecruitmentStageService companyRecruitmentStageService;

    // Constructor for dependency injection with Qualifier
    public AuthServiceImpl(
            AccountRepository accountRepository,
            CandidateRepository candidateRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RedisService redisService,
            MailService mailService,
            JwtDecoder jwtDecoder,
            @Qualifier("rawJwtDecoder") JwtDecoder rawJwtDecoder,
            GoogleAuthService googleAuthService,
            UserCacheService userCacheService,
            CompanyRecruitmentStageService companyRecruitmentStageService) {
        this.accountRepository = accountRepository;
        this.candidateRepository = candidateRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.redisService = redisService;
        this.mailService = mailService;
        this.jwtDecoder = jwtDecoder;
        this.rawJwtDecoder = rawJwtDecoder;
        this.googleAuthService = googleAuthService;
        this.userCacheService = userCacheService;
        this.companyRecruitmentStageService = companyRecruitmentStageService;
    }

    private Integer TIME_OTP_EXPIRED = 300;

    @Value("${jwt.valid-duration}")
    private long accessTtl;

    @Value("${jwt.refreshable-duration}")
    private Integer refreshTtl;

    @Override
    public void register(AuthRequests.RegisterRequest request, boolean isHR) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorType.CONFLICT, "User already existed with email");
        }

        // Create account
        Account account = Account.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(isHR ? Role.HR : Role.USER)
                .emailVerified(false)
                .build();
        if (!isHR) {
            // Create candidate
            Candidate candidate = Candidate.builder()
                    .account(account)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .build();
            account.setCandidate(candidate);

            candidateRepository.save(candidate);
        } else {
            // Create company - for HR
            Company company = Company.builder()
                    .account(account)

                    .build();
            account.setCompany(company);

            Company savedCompany = companyRepository.save(company);
            companyRecruitmentStageService.initializeDefaultStages(savedCompany);
        }
        sendOtp(normalizedEmail);
    }

    private void sendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        String otp = generateOtp();
        redisService.setObject(otpKey(normalizedEmail), otp, TIME_OTP_EXPIRED);
        log.info("OTP generated for {} (dev only log): {}", normalizedEmail, otp);
        mailService.sendOtp(normalizedEmail, otp);
    }

    @Override
    public void confirmRegisterOtp(AuthRequests.ConfirmOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateOtpOrThrow(normalizedEmail, request.getOtp());

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        if (account.isEmailVerified()) {
            redisService.deleteObject(otpKey(normalizedEmail));
            return;
        }

        account.setEmailVerified(true);
        accountRepository.save(account);
        redisService.deleteObject(otpKey(normalizedEmail));
    }

    @Override
    public String confirmResetPasswordOtp(AuthRequests.ConfirmOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateOtpOrThrow(normalizedEmail, request.getOtp());

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        if (!account.isEmailVerified()) {
            throw new AppException(ErrorType.BAD_REQUEST, "Email not verified");
        }

        redisService.deleteObject(otpKey(normalizedEmail));
        return jwtTokenService.generateResetPasswordToken(normalizedEmail);
    }

    @Override
    public Integer resendOtp(AuthRequests.ResendOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        sendOtp(normalizedEmail);
        return TIME_OTP_EXPIRED;
    }

    @Override
    public Long getTtlOtp(String email) {
        return redisService.getTtl(otpKey(normalizeEmail(email)));
    }

    @Override
    public AuthResponses.TokenResponse login(AuthRequests.LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));
        if (!account.getRole().toString().equalsIgnoreCase(request.getRole())) {
            throw new AppException(ErrorType.UNAUTHORIZED, "You do not have permission to log in to this account");
        }
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid password");
        }

        if (!account.isEmailVerified()) {
            sendOtp(normalizedEmail);
            Map<String, Object> meta = new HashMap<>();
            meta.put("expiredIn", String.valueOf(TIME_OTP_EXPIRED));
            throw new AppException(ErrorType.UNVERIFIED, "Email not verified", meta);
        }

        // Generate RT with family, then AT with familyId embedded
        String refreshToken = jwtTokenService.generateRefreshTokenWithFamily(account);
        Jwt rtJwt = rawJwtDecoder.decode(refreshToken);
        String familyId = rtJwt.getClaimAsString("fam");
        String accessToken = jwtTokenService.generateAccessToken(account, familyId);

        log.debug("Login successful for account {} with family {}", account.getId(), familyId);
        return AuthResponses.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    @Override
    public void logout(String accessToken) {
        // 1. Blacklist access token (best-effort, may be expired)
        String familyFromAt = null;
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Jwt jwt = rawJwtDecoder.decode(accessToken);
                String jti = jwt.getId();
                long remainingAt = jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
                if (remainingAt > 0) {
                    jwtTokenService.blacklist("bl-at", jti, remainingAt);
                }
                familyFromAt = jwt.getClaimAsString("fam");
            } catch (JwtException e) {
                log.debug("AT expired/invalid on logout — skipping AT blacklist: {}", e.getMessage());
            }
        }

        // 2. Block refresh token family
        blockFamily(familyFromAt);

        if (familyFromAt == null) {
            log.info("Logout with no valid tokens — client already cleared");
        }
    }

    private void blockFamily(String fam) {
        if (fam == null)
            return;
        Long famTtl = redisService.getTtl("rt:fam:exp:" + fam);
        if (famTtl != null && famTtl > 0) {
            redisService.setObject("rt:fam:blocked:" + fam, "1", famTtl.intValue());
            log.info("Logout: blocked RT family {} (TTL {}s)", fam, famTtl);
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
            jwtTokenService.blacklist("bl-rt", jti, (long) refreshTtl);

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
    public Integer forgotPassword(AuthRequests.ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Optional<Account> accountOpt = accountRepository.findByEmail(normalizedEmail);
        if (accountOpt.isEmpty())
            throw new AppException(ErrorType.NOT_FOUND, "User not registered yet");

        if (!accountOpt.get().isEmailVerified()) {
            throw new AppException(ErrorType.BAD_REQUEST, "Email not verified");
        }

        sendOtp(normalizedEmail);
        return TIME_OTP_EXPIRED;
    }

    @Override
    public void resetPassword(String resetPasswordToken, AuthRequests.ResetPasswordRequest request) {
        Jwt jwt;
        try {
            jwt = rawJwtDecoder.decode(resetPasswordToken);
        } catch (JwtException ex) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid or expired reset password token");
        }

        if (!"opt_token".equals(jwt.getClaimAsString("type"))) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid token type");
        }

        String email = normalizeEmail(jwt.getSubject());
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Invalid OTP"));
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        redisService.deleteObject(otpKey(email));
    }

    @Override
    public Integer requestEmailChangeOtp(String accountId, AuthRequests.RequestEmailChangeOtp request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        String normalizedNewEmail = normalizeEmail(request.getNewEmail());
        if (Objects.equals(account.getEmail(), normalizedNewEmail)) {
            throw new AppException(ErrorType.BAD_REQUEST, "New email must be different from current email");
        }
        if (accountRepository.existsByEmail(normalizedNewEmail)) {
            throw new AppException(ErrorType.CONFLICT, "Email already in use");
        }

        String otp = generateOtp();
        redisService.setObject(accountOtpKey(accountId, "email_change"), otp, TIME_OTP_EXPIRED);
        redisService.setObject(accountPendingKey(accountId, "email_change"), normalizedNewEmail, TIME_OTP_EXPIRED);

        mailService.sendOtp(normalizedNewEmail, otp);
        return TIME_OTP_EXPIRED;
    }

    @Override
    public void confirmEmailChange(String accountId, AuthRequests.ConfirmEmailChangeRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        String pendingEmail = redisService.getObject(accountPendingKey(accountId, "email_change"), String.class);
        if (pendingEmail == null) {
            throw new AppException(ErrorType.BAD_REQUEST, "No pending email change request");
        }

        String normalizedNewEmail = normalizeEmail(request.getNewEmail());
        if (!pendingEmail.equals(normalizedNewEmail)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Email does not match pending request");
        }

        validateAccountOtpOrThrow(accountId, "email_change", request.getOtp());

        if (accountRepository.existsByEmail(normalizedNewEmail)) {
            throw new AppException(ErrorType.CONFLICT, "Email already in use");
        }

        account.setEmail(normalizedNewEmail);
        account.setEmailVerified(true);
        accountRepository.save(account);

        redisService.deleteObject(accountOtpKey(accountId, "email_change"));
        redisService.deleteObject(accountPendingKey(accountId, "email_change"));
    }

    @Override
    public Integer requestPasswordChangeOtp(String accountId, AuthRequests.RequestPasswordChangeOtp request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Current password is invalid");
        }
        if (passwordEncoder.matches(request.getNewPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorType.BAD_REQUEST, "New password must be different from current password");
        }

        String otp = generateOtp();
        String nextPasswordHash = passwordEncoder.encode(request.getNewPassword());

        redisService.setObject(accountOtpKey(accountId, "password_change"), otp, TIME_OTP_EXPIRED);
        redisService.setObject(accountPendingKey(accountId, "password_change"), nextPasswordHash, TIME_OTP_EXPIRED);
        mailService.sendOtp(account.getEmail(), otp);
        return TIME_OTP_EXPIRED;
    }

    @Override
    public void confirmPasswordChange(String accountId, AuthRequests.ConfirmPasswordChangeRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        String nextPasswordHash = redisService.getObject(accountPendingKey(accountId, "password_change"), String.class);
        if (nextPasswordHash == null || nextPasswordHash.isBlank()) {
            throw new AppException(ErrorType.BAD_REQUEST, "No pending password change request");
        }

        validateAccountOtpOrThrow(accountId, "password_change", request.getOtp());

        account.setPasswordHash(nextPasswordHash);
        accountRepository.save(account);

        redisService.deleteObject(accountOtpKey(accountId, "password_change"));
        redisService.deleteObject(accountPendingKey(accountId, "password_change"));
    }

    @Override
    public AuthResponses.TokenResponse googleLogin(AuthRequests.GoogleLoginRequest request) {
        try {
            // Verify Google ID token using GoogleAuthService
            GoogleUserInfo googleUser = googleAuthService.verify(request.getIdToken());

            String email = normalizeEmail(googleUser.email());
            boolean emailVerified = Boolean.TRUE.equals(googleUser.emailVerified());
            String givenName = googleUser.givenName();
            String familyName = googleUser.familyName();
            String pictureUrl = googleUser.picture();

            boolean isHR = "HR".equalsIgnoreCase(request.getRole());
            Role targetRole = isHR ? Role.HR : Role.USER;

            Account account = accountRepository.findByEmail(email).orElse(null);
            if (account == null) {
                // Create new account
                account = Account.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("google:" + email + ":" + System.currentTimeMillis()))
                        .role(targetRole)
                        .emailVerified(emailVerified)
                        .build();
                if (isHR) {
                    Company company = Company.builder().account(account).build();
                    account.setCompany(company);
                    companyRepository.save(company);
                } else {
                    Candidate candidate = Candidate.builder()
                            .account(account)
                            .firstName(givenName)
                            .lastName(familyName)
                            .avatar(pictureUrl)
                            .build();
                    account.setCandidate(candidate);
                    candidateRepository.save(candidate);
                }
                log.info("Created new {} account from Google OAuth: {}", targetRole, email);
            } else {
                // Existing account: verify role matches and use current DB record as-is.
                if (!account.getRole().equals(targetRole)) {
                    throw new AppException(ErrorType.UNAUTHORIZED,
                            "You do not have permission to log in to this account");
                }
            }

            // Generate family-based tokens
            String refreshToken = jwtTokenService.generateRefreshTokenWithFamily(account);
            Jwt rtJwt = rawJwtDecoder.decode(refreshToken);
            String familyId = rtJwt.getClaimAsString("fam");
            String accessToken = jwtTokenService.generateAccessToken(account, familyId);

            log.info("Google login successful for account {}", account.getId());
            return AuthResponses.TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(accessTtl)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process Google login: {}", e.getMessage());
            throw new AppException(ErrorType.UNAUTHORIZED, "Failed to verify Google token");
        }
    }

    @Override
    public AuthResponses.TokenResponse refreshWithFamily(Jwt jwt) {
        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw new AppException(ErrorType.UNAUTHORIZED, "Invalid token type");
        }
        String jti = jwt.getId();
        String fam = jwt.getClaimAsString("fam");
        String accountId = jwt.getSubject();
        String tokenRole = jwt.getClaimAsString("role");

        log.debug("Processing refresh token - jti: {}, family: {}, accountId: {}", jti, fam, accountId);

        // 1) Check if family is blocked (logout or reuse detected)
        if (redisService.exists("rt:fam:blocked:" + fam)) {
            log.warn("Refresh attempt with blocked family: {}", fam);
            throw new AppException(ErrorType.UNAUTHORIZED, "Session has been invalidated");
        }

        // 2) Check if user's role has changed by comparing token role vs current DB
        // role
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND, "Account not found"));

        if (tokenRole != null && !tokenRole.equals(account.getRole().name())) {
            log.warn("Role changed detected for account {} - token role: {}, current role: {} - blocking family {}",
                    accountId, tokenRole, account.getRole(), fam);

            // Block the family to prevent further refresh attempts
            long remainingTtl = jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
            if (remainingTtl > 0) {
                redisService.setObject("rt:fam:blocked:" + fam, "1", (int) remainingTtl);
            }

            // Evict user cache
            userCacheService.evict(accountId);

            throw new AppException(ErrorType.UNAUTHORIZED,
                    "Your role has been changed from " + tokenRole + " to " + account.getRole().name()
                            + ". Please login again.");
        }

        // 3) Check Redis for token metadata
        var meta = redisService.getObject("rt:jti:" + jti, Map.class);

        if (meta == null || Boolean.TRUE.equals(meta.get("revoked"))) {
            // REUSE DETECTED or already revoked: block entire family
            log.warn("Refresh token reuse or revocation detected - jti: {}, family: {}", jti, fam);

            String current = redisService.getObject("rt:fam:current:" + fam, String.class);
            if (current != null) {
                // Block the family to prevent further use
                redisService.setObject("rt:fam:blocked:" + fam, "1", refreshTtl);
            }

            throw new AppException(ErrorType.UNAUTHORIZED, "Session has been invalidated");
        }

        // 4) Success: revoke old RT and rotate
        redisService.setField("rt:jti:" + jti, "revoked", true);

        String refreshToken = jwtTokenService.rotateRefreshToken(account, fam);
        String accessToken = jwtTokenService.generateAccessToken(account, fam);

        log.debug("Token refresh successful for account {} in family {}", accountId, fam);

        return AuthResponses.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    @Override
    public String generateOTPToken(String email) {
        return "";
    }

    /*
     * ============================================ HELPER METHOD
     * ============================================
     */

    private String otpKey(String email) {
        return "otp:" + normalizeEmail(email);
    }

    private void validateOtpOrThrow(String email, String otp) {
        String cachedOtp = redisService.getObject(otpKey(email), String.class);
        if (cachedOtp == null) {
            throw new AppException(ErrorType.BAD_REQUEST, "OTP expired or not found");
        }
        if (otp == null || !cachedOtp.equals(otp.trim())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Invalid OTP");
        }
    }

    private String accountOtpKey(String accountId, String purpose) {
        return "otp:account:" + accountId + ":" + purpose;
    }

    private String accountPendingKey(String accountId, String purpose) {
        return "otp:account:pending:" + accountId + ":" + purpose;
    }

    private void validateAccountOtpOrThrow(String accountId, String purpose, String otp) {
        String cachedOtp = redisService.getObject(accountOtpKey(accountId, purpose), String.class);
        if (cachedOtp == null) {
            throw new AppException(ErrorType.BAD_REQUEST, "OTP expired or not found");
        }
        if (otp == null || !cachedOtp.equals(otp.trim())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Invalid OTP");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int value = 100000 + random.nextInt(900000);
        return String.valueOf(value);
    }
}
