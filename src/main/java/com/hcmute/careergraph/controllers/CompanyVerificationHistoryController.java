package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import com.hcmute.careergraph.services.CompanyVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("companies/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class CompanyVerificationHistoryController {

    private final CompanyVerificationService companyVerificationService;
    private final SecurityUtils securityUtils;

    @GetMapping("/verification-requests")
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
