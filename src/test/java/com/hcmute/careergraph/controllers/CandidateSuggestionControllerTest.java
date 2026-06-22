package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CandidateESService;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateSuggestionControllerTest {

    @Mock
    private CandidateESService candidateESService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyAccessPolicyService companyAccessPolicyService;
    @Mock
    private Authentication authentication;

    private CandidateSuggestionController controller;

    @BeforeEach
    void setUp() {
        controller = new CandidateSuggestionController(
                candidateESService,
                securityUtils,
                companyRepository,
                companyAccessPolicyService);
    }

    @Test
    void searchCandidates_shouldRejectCompanyWithoutCandidateSearchAccess() {
        Company company = new Company();
        company.setId("company-1");

        when(securityUtils.extractCompanyId(authentication)).thenReturn("company-1");
        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));

        BadRequestException exception = new BadRequestException("blocked");
        org.mockito.Mockito.doThrow(exception)
                .when(companyAccessPolicyService)
                .assertCompanyCanSearchCandidates(company);

        assertThatThrownBy(() -> controller.searchCandidates("", null, 0, 10, authentication))
                .isSameAs(exception);

        verify(candidateESService, never()).searchCandidatesForCompany(any(), any(), any());
        verify(candidateESService, never()).hybridSearchCandidates(any(), any(), any());
    }

    @Test
    void searchCandidates_shouldUseCompanyScopedSearchWhenAccessAllowedAndKeywordEmpty() {
        Company company = new Company();
        company.setId("company-1");

        when(securityUtils.extractCompanyId(authentication)).thenReturn("company-1");
        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));
        when(candidateESService.searchCandidatesForCompany(eq("company-1"), any(), any())).thenReturn(null);

        controller.searchCandidates("", null, 0, 10, authentication);

        verify(companyAccessPolicyService).assertCompanyCanSearchCandidates(company);
        verify(candidateESService).searchCandidatesForCompany(eq("company-1"), any(), any());
    }
}
