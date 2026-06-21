package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.CompanyVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("companies/me/verification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class CompanyVerificationController {

    private final CompanyVerificationService companyVerificationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public RestResponse<CompanyVerificationResponses.CompanyVerificationStatusResponse> getMyVerification(
            Authentication authentication) {
        String companyId = securityUtils.extractCompanyId(authentication);
        if (companyId == null || companyId.isBlank()) {
            throw new BadRequestException("Company ID invalid");
        }

        return RestResponse.<CompanyVerificationResponses.CompanyVerificationStatusResponse>builder()
                .status(HttpStatus.OK)
                .data(companyVerificationService.getMyVerification(companyId))
                .build();
    }

    @PostMapping
    public RestResponse<CompanyVerificationResponses.CompanyVerificationStatusResponse> submitVerification(
            Authentication authentication,
            @Valid @RequestBody CompanyVerificationRequests.SubmitVerificationRequest request) {
        String companyId = securityUtils.extractCompanyId(authentication);
        Account currentAccount = securityUtils.getCurrentAccount()
                .orElseThrow(() -> new BadRequestException("Account invalid"));

        return RestResponse.<CompanyVerificationResponses.CompanyVerificationStatusResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Verification request submitted successfully")
                .data(companyVerificationService.submitVerification(companyId, currentAccount.getId(), request))
                .build();
    }

    @PutMapping("/{requestId}")
    public RestResponse<CompanyVerificationResponses.CompanyVerificationStatusResponse> updateVerification(
            Authentication authentication,
            @PathVariable String requestId,
            @Valid @RequestBody CompanyVerificationRequests.SubmitVerificationRequest request) {
        String companyId = securityUtils.extractCompanyId(authentication);
        Account currentAccount = securityUtils.getCurrentAccount()
                .orElseThrow(() -> new BadRequestException("Account invalid"));

        return RestResponse.<CompanyVerificationResponses.CompanyVerificationStatusResponse>builder()
                .status(HttpStatus.OK)
                .message("Verification request updated successfully")
                .data(companyVerificationService.updateVerification(companyId, requestId, currentAccount.getId(), request))
                .build();
    }

    @GetMapping("/requests")
    public RestResponse<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>> listMyVerificationRequests(
            Authentication authentication) {
        String companyId = securityUtils.extractCompanyId(authentication);
        if (companyId == null || companyId.isBlank()) {
            throw new BadRequestException("Company ID invalid");
        }

        return RestResponse.<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>>builder()
                .status(HttpStatus.OK)
                .data(companyVerificationService.listMyVerificationRequests(companyId))
                .build();
    }
}
