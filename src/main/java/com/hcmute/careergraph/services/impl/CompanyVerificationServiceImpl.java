package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyVerificationDocument;
import com.hcmute.careergraph.persistence.models.CompanyVerificationRequest;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.CompanyVerificationRequestRepository;
import com.hcmute.careergraph.services.CompanyVerificationService;
import com.hcmute.careergraph.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyVerificationServiceImpl implements CompanyVerificationService {

    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final CompanyVerificationRequestRepository verificationRequestRepository;
    private final CompanyVerificationMapperSupport mapperSupport;
    private final JobService jobService;

    @Override
    @Transactional(readOnly = true)
    public CompanyVerificationResponses.CompanyVerificationStatusResponse getMyVerification(String companyId) {
        Company company = findCompany(companyId);
        CompanyVerificationRequest latestRequest = verificationRequestRepository
                .findTopByCompanyIdOrderBySubmittedAtDescCreatedDateDesc(companyId)
                .orElse(null);
        return mapperSupport.toStatusResponse(company, latestRequest);
    }

    @Override
    public CompanyVerificationResponses.CompanyVerificationStatusResponse submitVerification(
            String companyId,
            String accountId,
            CompanyVerificationRequests.SubmitVerificationRequest request) {
        Company company = findCompany(companyId);
        validateSubmissionAllowed(company);
        Account account = findAccount(accountId);

        CompanyVerificationRequest savedRequest = verificationRequestRepository.save(buildOrRefreshRequest(
                new CompanyVerificationRequest(),
                company,
                account,
                request));
        applyCompanyVerificationSnapshot(company, savedRequest, null);
        companyRepository.save(company);
        jobService.syncCompanyJobsSearchDocuments(companyId);
        return mapperSupport.toStatusResponse(company, savedRequest);
    }

    @Override
    public CompanyVerificationResponses.CompanyVerificationStatusResponse updateVerification(
            String companyId,
            String requestId,
            String accountId,
            CompanyVerificationRequests.SubmitVerificationRequest request) {
        Company company = findCompany(companyId);
        CompanyVerificationRequest verificationRequest = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Verification request not found"));
        if (!companyId.equals(verificationRequest.getCompany().getId())) {
            throw new BadRequestException("Verification request does not belong to this company");
        }
        if (verificationRequest.getVerificationStatus() != CompanyVerificationStatus.REJECTED
                && verificationRequest.getVerificationStatus() != CompanyVerificationStatus.NEEDS_ADDITIONAL_INFO) {
            throw new BadRequestException("Verification request cannot be updated in the current state");
        }

        Account account = findAccount(accountId);
        CompanyVerificationRequest savedRequest = verificationRequestRepository.save(buildOrRefreshRequest(
                new CompanyVerificationRequest(),
                company,
                account,
                request));
        applyCompanyVerificationSnapshot(company, savedRequest, null);
        companyRepository.save(company);
        jobService.syncCompanyJobsSearchDocuments(companyId);
        return mapperSupport.toStatusResponse(company, savedRequest);
    }

    private CompanyVerificationRequest buildOrRefreshRequest(
            CompanyVerificationRequest verificationRequest,
            Company company,
            Account account,
            CompanyVerificationRequests.SubmitVerificationRequest request) {
        verificationRequest.setCompany(company);
        verificationRequest.setVerificationStatus(CompanyVerificationStatus.PENDING_REVIEW);
        verificationRequest.setTaxCode(request.taxCode().trim());
        verificationRequest.setCompanyName(request.companyName().trim());
        verificationRequest.setLegalRepresentativeName(request.legalRepresentativeName().trim());
        verificationRequest.setBusinessEmail(request.businessEmail().trim());
        verificationRequest.setWebsite(trimToNull(request.website()));
        verificationRequest.setSubmittedByAccount(account);
        verificationRequest.setSubmittedAt(LocalDateTime.now());
        verificationRequest.setReviewedByAccount(null);
        verificationRequest.setReviewedAt(null);
        verificationRequest.setAdminNote(null);
        verificationRequest.replaceDocuments(toDocuments(request.documents()));
        return verificationRequest;
    }

    private Set<CompanyVerificationDocument> toDocuments(
            java.util.List<CompanyVerificationRequests.VerificationDocumentRequest> documents) {
        Set<CompanyVerificationDocument> mapped = new LinkedHashSet<>();
        if (documents == null) {
            return mapped;
        }
        documents.forEach(document -> mapped.add(CompanyVerificationDocument.builder()
                .documentUrl(document.documentUrl().trim())
                .documentType(trimToNull(document.documentType()))
                .originalFileName(trimToNull(document.originalFileName()))
                .mimeType(trimToNull(document.mimeType()))
                .build()));
        return mapped;
    }

    private void validateSubmissionAllowed(Company company) {
        if (company.getVerificationStatus() == CompanyVerificationStatus.PENDING_REVIEW) {
            throw new BadRequestException("Company verification is already pending review");
        }
        if (company.getVerificationStatus() == CompanyVerificationStatus.APPROVED) {
            throw new BadRequestException("Company is already verified");
        }
    }

    private void applyCompanyVerificationSnapshot(
            Company company,
            CompanyVerificationRequest request,
            String adminNote) {
        company.setTaxCode(request.getTaxCode());
        company.setName(request.getCompanyName());
        company.setLegalRepresentativeName(request.getLegalRepresentativeName());
        company.setVerificationBusinessEmail(request.getBusinessEmail());
        company.setVerificationWebsite(request.getWebsite());
        company.setVerificationStatus(request.getVerificationStatus());
        company.setVerificationSubmittedAt(request.getSubmittedAt());
        company.setVerificationReviewedAt(request.getReviewedAt());
        company.setVerificationReviewedByAccount(request.getReviewedByAccount());
        company.setVerificationAdminNote(adminNote);
    }

    private Company findCompany(String companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    private Account findAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> listMyVerificationRequests(String companyId) {
        Company company = findCompany(companyId);
        java.util.List<CompanyVerificationRequest> requests = verificationRequestRepository
                .findByCompanyIdOrderByCreatedDateDesc(companyId);
        return requests.stream()
                .map(mapperSupport::toSummaryResponse)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
