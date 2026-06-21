package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.AdminCompanyVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyController {

    private final AdminCompanyVerificationService adminCompanyVerificationService;
    private final SecurityUtils securityUtils;

    @GetMapping("/company-verification-requests")
    public RestResponse<Page<CompanyVerificationResponses.VerificationRequestSummaryResponse>> getVerificationRequests(
            @RequestParam(required = false) CompanyVerificationStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return RestResponse.<Page<CompanyVerificationResponses.VerificationRequestSummaryResponse>>builder()
                .status(HttpStatus.OK)
                .data(adminCompanyVerificationService.getVerificationRequests(status, query, pageable))
                .build();
    }

    @GetMapping("/companies")
    public RestResponse<Page<CompanyVerificationResponses.AdminCompanyListItemResponse>> getCompanies(
            @RequestParam(required = false) CompanyVerificationStatus verificationStatus,
            @RequestParam(required = false) CompanyOperationalStatus operationalStatus,
            @RequestParam(required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return RestResponse.<Page<CompanyVerificationResponses.AdminCompanyListItemResponse>>builder()
                .status(HttpStatus.OK)
                .data(adminCompanyVerificationService.getCompanies(
                        verificationStatus,
                        operationalStatus,
                        query,
                        pageable))
                .build();
    }

    @GetMapping("/dashboard-summary")
    public RestResponse<CompanyVerificationResponses.AdminDashboardSummaryResponse> getDashboardSummary() {
        return RestResponse.<CompanyVerificationResponses.AdminDashboardSummaryResponse>builder()
                .status(HttpStatus.OK)
                .data(adminCompanyVerificationService.getDashboardSummary())
                .build();
    }

    @GetMapping("/companies/{companyId}")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> getCompanyDetail(
            @PathVariable String companyId) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .data(adminCompanyVerificationService.getCompanyDetail(companyId))
                .build();
    }

    @GetMapping("/company-verification-requests/{requestId}")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> getVerificationRequestDetail(
            @PathVariable String requestId) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .data(adminCompanyVerificationService.getVerificationRequestDetail(requestId))
                .build();
    }

    @PostMapping("/company-verification-requests/{requestId}/approve")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> approveRequest(
            @PathVariable String requestId,
            @Valid @RequestBody CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .message("Verification request approved successfully")
                .data(adminCompanyVerificationService.approveRequest(requestId, currentAccountId(), request))
                .build();
    }

    @PostMapping("/company-verification-requests/{requestId}/reject")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> rejectRequest(
            @PathVariable String requestId,
            @Valid @RequestBody CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .message("Verification request rejected successfully")
                .data(adminCompanyVerificationService.rejectRequest(requestId, currentAccountId(), request))
                .build();
    }

    @PostMapping("/company-verification-requests/{requestId}/request-additional-info")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> requestAdditionalInfo(
            @PathVariable String requestId,
            @Valid @RequestBody CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .message("Requested additional verification information successfully")
                .data(adminCompanyVerificationService.requestAdditionalInfo(requestId, currentAccountId(), request))
                .build();
    }

    @PostMapping("/companies/{companyId}/block")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> blockCompany(
            @PathVariable String companyId,
            @Valid @RequestBody CompanyVerificationRequests.AdminCompanyBlockRequest request) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .message("Company blocked successfully")
                .data(adminCompanyVerificationService.blockCompany(companyId, currentAccountId(), request))
                .build();
    }

    @PostMapping("/companies/{companyId}/unblock")
    public RestResponse<CompanyVerificationResponses.VerificationRequestDetailResponse> unblockCompany(
            @PathVariable String companyId,
            @Valid @RequestBody CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        return RestResponse.<CompanyVerificationResponses.VerificationRequestDetailResponse>builder()
                .status(HttpStatus.OK)
                .message("Company unblocked successfully")
                .data(adminCompanyVerificationService.unblockCompany(companyId, currentAccountId(), request))
                .build();
    }

    @GetMapping("/companies/{companyId}/verification-requests")
    public RestResponse<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>> getCompanyVerificationHistory(
            @PathVariable String companyId) {
        return RestResponse.<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>>builder()
                .status(HttpStatus.OK)
                .data(adminCompanyVerificationService.getCompanyVerificationHistory(companyId))
                .build();
    }

    private String currentAccountId() {
        Account currentAccount = securityUtils.getCurrentAccount()
                .orElseThrow(() -> new com.hcmute.careergraph.exception.BadRequestException("Account invalid"));
        return currentAccount.getId();
    }
}
