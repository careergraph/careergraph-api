package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.persistence.dtos.request.AuthRequests;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.GoogleAuthService;
import com.hcmute.careergraph.services.JwtTokenService;
import com.hcmute.careergraph.services.MailService;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.services.UserCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private RedisService redisService;
    @Mock
    private MailService mailService;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private JwtDecoder rawJwtDecoder;
    @Mock
    private GoogleAuthService googleAuthService;
    @Mock
    private UserCacheService userCacheService;
    @Mock
    private CompanyRecruitmentStageService companyRecruitmentStageService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                accountRepository,
                candidateRepository,
                companyRepository,
                passwordEncoder,
                jwtTokenService,
                redisService,
                mailService,
                jwtDecoder,
                rawJwtDecoder,
                googleAuthService,
                userCacheService,
                companyRecruitmentStageService);
    }

    @Test
    void registerHr_shouldCreateCompanyWithVerificationDefaults() {
        AuthRequests.RegisterRequest request = new AuthRequests.RegisterRequest();
        request.setEmail("hr@careergraph.com");
        request.setPassword("secret");

        when(accountRepository.existsByEmail("hr@careergraph.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId("company-1");
            return company;
        });

        authService.register(request, true);

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        Company savedCompany = companyCaptor.getValue();
        assertThat(savedCompany.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.NOT_SUBMITTED);
        assertThat(savedCompany.getOperationalStatus()).isEqualTo(CompanyOperationalStatus.ACTIVE);
        verify(companyRecruitmentStageService).initializeDefaultStages(any(Company.class));
        verify(candidateRepository, never()).save(any());
    }
}
