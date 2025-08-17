package com.hcmute.careergraph.services.impl;


import com.hcmute.careergraph.dtos.request.auth.IntrospectRequest;
import com.hcmute.careergraph.dtos.request.auth.LoginRequest;
import com.hcmute.careergraph.dtos.request.auth.LogoutRequest;
import com.hcmute.careergraph.dtos.request.auth.RefreshRequest;
import com.hcmute.careergraph.dtos.response.auth.IntrospectResponse;
import com.hcmute.careergraph.dtos.response.auth.LoginResponse;
import com.hcmute.careergraph.entities.mysql.Token;

import com.hcmute.careergraph.entities.mysql.User;
import com.hcmute.careergraph.enums.ErrorType;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.repository.sql.TokenRepository;
import com.hcmute.careergraph.repository.sql.UserRepository;
import com.hcmute.careergraph.services.IAuthService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class AuthService implements IAuthService {

    @Value("${jwt.signer-key}")
    private String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    private long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    private long REFRESHABLE_DURATION;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        String token = request.getToken();
        var isValid = true;

        verifyToken(token, false);

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // Login
    @Override
    public LoginResponse login(LoginRequest request)
            throws ChangeSetPersister.NotFoundException {

        boolean isExisted = userRepository.existsByUsername(request.getUsername());
        if (!isExisted) {
            throw new ChangeSetPersister.NotFoundException();
        }

        User existedUser = userRepository.findByUsername(request.getUsername()).get();

        if (!passwordEncoder.matches(request.getPassword(), existedUser.getPassword())) {
            throw new AppException(ErrorType.UNAUTHORIZED);
        }

        String token = generateToken(existedUser);

        if (token != null && !token.isEmpty()) {
            return LoginResponse.builder()
                    .token(token)
                    .build();
        } else {
            throw new AppException(ErrorType.UNAUTHORIZED);
        }
    }

    // Refresh token
    @Override
    public String refreshToken(RefreshRequest request)
            throws ParseException, ChangeSetPersister.NotFoundException {

        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        String jit = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
        Token recentToken = Token.builder()
                .id(jit)
                .expiryTime(expiration)
                .build();

        try {
            tokenRepository.save(recentToken);
        } catch (RuntimeException e) {
            throw new AppException(ErrorType.UNSAVED_DATA);
        }

        String username = signedJWT.getJWTClaimsSet().getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException());

        String newToken = generateToken(user);

        if (newToken != null && !newToken.isEmpty()) {
            return newToken;
        } else {
            throw new AppException(ErrorType.UNAUTHORIZED);
        }
    }

    @Override
    public void logout(LogoutRequest request) throws ParseException {

        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        String jit = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();

        Token token = Token.builder()
                .id(jit)
                .expiryTime(expiration)
                .build();

        try {
            tokenRepository.save(token);
            log.info("Token: {}", token);

        } catch (RuntimeException e) {
            throw new AppException(ErrorType.UNSAVED_DATA);
        }
    }

    // Generate token
    private String generateToken(User user) {

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("system")
                .issueTime(new Date())
                .claim("role", "CUSTOMER")
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    // Get info and verify token
    private SignedJWT verifyToken(String token, boolean isRefresh) {
        try {
            JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            Date expTime = (isRefresh)
                    ? new Date(signedJWT
                    .getJWTClaimsSet()
                    .getIssueTime()
                    .toInstant()
                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                    .toEpochMilli())
                    : signedJWT.getJWTClaimsSet().getExpirationTime();
            var verified = signedJWT.verify(verifier);

            if (!verified || !expTime.after(new Date())) {
                throw new AppException(ErrorType.UNAUTHORIZED);
            }

            // Check token logout
            String jit = signedJWT.getJWTClaimsSet().getJWTID();
            boolean isExisted = tokenRepository.existsById(jit);

            if (isExisted) {
                throw new AppException(ErrorType.UNAUTHORIZED);
            }

            return signedJWT;

        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorType.UNAUTHORIZED);
        }
    }
}
