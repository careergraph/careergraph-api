package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CompanyMapper;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequests;
import com.hcmute.careergraph.persistence.dtos.request.CompanyStageRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyRecruitmentStageResponse;
import com.hcmute.careergraph.persistence.dtos.response.CompanyResponse;
import com.hcmute.careergraph.persistence.dtos.response.JobResponse;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.CompanyRecruitmentStage;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.CompanyService;
import com.hcmute.careergraph.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final JobService jobService;
    private final SecurityUtils securityUtils;
    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;
    private final CompanyRecruitmentStageService companyRecruitmentStageService;

    @GetMapping("/me")
    public RestResponse<CompanyResponse> getCompanyProfile(Authentication authentication) {
        Company company = companyService.getCompanyById(securityUtils.extractCompanyId(authentication));

        return RestResponse.<CompanyResponse>builder()
                .status(HttpStatus.OK)
                .data(companyMapper.toResponse(company, false))
                .build();
    }

            @GetMapping("/me/recruitment-stages")
            public RestResponse<List<CompanyRecruitmentStageResponse>> getMyRecruitmentStages(
                Authentication authentication
            ) {
            String companyId = securityUtils.extractCompanyId(authentication);
            if (companyId == null || companyId.isBlank()) {
                throw new BadRequestException("Company ID invalid");
            }

            List<CompanyRecruitmentStage> stages = companyRecruitmentStageService.getCompanyStages(companyId);
            List<CompanyRecruitmentStageResponse> data = stages.stream()
                .map(stage -> CompanyRecruitmentStageResponse.builder()
                    .stage(stage.getStage())
                    .label(stage.getStage() != null ? stage.getStage().getLabel() : null)
                    .displayOrder(stage.getDisplayOrder())
                    .active(stage.isActive())
                    .required(stage.getStage() != null
                        && com.hcmute.careergraph.enums.application.ApplicationStage
                            .isRequiredStage(stage.getStage()))
                    .build())
                .toList();

            return RestResponse.<List<CompanyRecruitmentStageResponse>>builder()
                .status(HttpStatus.OK)
                .data(data)
                .build();
            }

            @PutMapping("/me/recruitment-stages")
            public RestResponse<List<CompanyRecruitmentStageResponse>> updateMyRecruitmentStages(
                Authentication authentication,
                @Valid @RequestBody CompanyStageRequests.UpdateRecruitmentStagesRequest request
            ) {
            String companyId = securityUtils.extractCompanyId(authentication);
            if (companyId == null || companyId.isBlank()) {
                throw new BadRequestException("Company ID invalid");
            }

            List<CompanyRecruitmentStage> updated = companyRecruitmentStageService
                .updateCompanyStages(companyId, request.stages());

            List<CompanyRecruitmentStageResponse> data = updated.stream()
                .map(stage -> CompanyRecruitmentStageResponse.builder()
                    .stage(stage.getStage())
                    .label(stage.getStage() != null ? stage.getStage().getLabel() : null)
                    .displayOrder(stage.getDisplayOrder())
                    .active(stage.isActive())
                    .required(stage.getStage() != null
                        && com.hcmute.careergraph.enums.application.ApplicationStage
                            .isRequiredStage(stage.getStage()))
                    .build())
                .toList();

            return RestResponse.<List<CompanyRecruitmentStageResponse>>builder()
                .status(HttpStatus.OK)
                .message("Recruitment stages updated successfully")
                .data(data)
                .build();
            }

    @PutMapping("/me/profile")
    public RestResponse<CompanyResponse> updateCompanyProfile(
            Authentication authentication,
            @Valid @RequestBody CompanyRequests.UpdateMyProfileRequest request
    ) {
        String companyId = securityUtils.extractCompanyId(authentication);
        if (companyId == null || companyId.isBlank()) {
            throw new BadRequestException("Company ID invalid");
        }

        Company company = companyService.updateMyProfile(companyId, request);
        return RestResponse.<CompanyResponse>builder()
                .status(HttpStatus.OK)
                .message("Company profile updated successfully")
                .data(companyMapper.toResponse(company, true))
                .build();
    }

    @GetMapping("/lookup")
    public RestResponse<List<HashMap<String,String>>> lookup(@RequestParam(required = false)String query) {
        List<HashMap<String,String>> result = companyService.lookup(query);
        return RestResponse.<List<HashMap<String, String>>>builder()
                .status(HttpStatus.OK)
                .data(result)
                .build();

    }

    @GetMapping("/{companyId:[0-9a-fA-F\\-]{36}}")
    public RestResponse<CompanyResponse> getCompanyDetail(@PathVariable("companyId") String companyId) {

        if (companyId == null || companyId.isEmpty()) {
            throw new BadRequestException("Company ID invalid");
        }

        Company company = companyService.getCompanyById(companyId);

        return RestResponse.<CompanyResponse>builder()
                .status(HttpStatus.OK)
                .data(companyMapper.toResponse(company, true))
                .build();
    }

    @GetMapping("/{companyId}/follow-status")
    public RestResponse<Boolean> getFollowStatus(
            @PathVariable("companyId") String companyId,
            Authentication authentication
    ) {
        if (companyId == null || companyId.isBlank()) {
            throw new BadRequestException("Company ID invalid");
        }

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (candidateId == null || candidateId.isBlank()) {
            throw new BadRequestException("Candidate ID invalid");
        }

        boolean followed = companyService.isCandidateFollowingCompany(candidateId, companyId);

        return RestResponse.<Boolean>builder()
                .status(HttpStatus.OK)
                .data(followed)
                .build();
    }

    @PutMapping("/{companyId}/follow")
    public RestResponse<Boolean> toggleFollowCompany(
            @PathVariable("companyId") String companyId,
            Authentication authentication
    ) {
        if (companyId == null || companyId.isBlank()) {
            throw new BadRequestException("Company ID invalid");
        }

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (candidateId == null || candidateId.isBlank()) {
            throw new BadRequestException("Candidate ID invalid");
        }

        boolean followed = companyService.toggleCandidateFollowCompany(candidateId, companyId);

        return RestResponse.<Boolean>builder()
                .status(HttpStatus.OK)
                .message(followed ? "Followed company successfully" : "Unfollowed company successfully")
                .data(followed)
                .build();
    }

    @GetMapping("/following")
    public RestResponse<List<CompanyResponse>> getFollowedCompanies(Authentication authentication) {
        String candidateId = securityUtils.extractCandidateId(authentication);
        if (candidateId == null || candidateId.isBlank()) {
            throw new BadRequestException("Candidate ID invalid");
        }

        List<CompanyResponse> companies = companyService.getFollowedCompanies(candidateId);

        return RestResponse.<List<CompanyResponse>>builder()
                .status(HttpStatus.OK)
                .data(companies)
                .build();
    }

    @GetMapping("/{companyId}/jobs")
    public RestResponse<Page<JobResponse>> getJobsByCompanies(@PathVariable("companyId") String companyId,
                                                              @RequestParam(name = "page", defaultValue = "0") Integer page,
                                                              @RequestParam(name = "size", defaultValue = "100") Integer size) {

        if (companyId == null || companyId.isEmpty()) {
            throw new BadRequestException("Company ID invalid");
        }

        Pageable pageable = null;
        if (size == null) {
            pageable = PageRequest.of(0, 100);
        } else {
            pageable = PageRequest.of(page, size);
        }

        Page<Job> jobs = jobService.getPublicJobsByCompany(companyId, pageable);

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .data(mapToJobResponsePage(jobs, pageable))
                .build();
    }

    // Helper method to map to response page
    private Page<JobResponse> mapToJobResponsePage(Page<Job> jobPage, Pageable pageable) {
        List<JobResponse> jobResponses = jobPage.stream()
                .map(job -> jobMapper.toResponse(job))
                .toList();
        return new PageImpl<>(jobResponses, pageable, jobPage.getTotalElements());
    }
}
