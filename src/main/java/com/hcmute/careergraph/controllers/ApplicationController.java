package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.ApplicationMapper;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationStageUpdateRequest;
import com.hcmute.careergraph.persistence.dtos.response.ApplicationResponse;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.services.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;
    private final SecurityUtils securityUtils;

    @PostMapping
    public RestResponse<ApplicationResponse> createApplication(@RequestBody ApplicationRequest request, Authentication authentication) {

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (candidateId == null || candidateId.isBlank()) {
            throw new BadRequestException("Candidate ID invalid");
        }
        request.setCandidateId(candidateId);

        if (request.getResumeUrl().isBlank()) {
            throw new BadRequestException("Application invalid. Resume is required");
        }

        Application application = applicationService.createApplication(request);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Application created successfully")
                .data(applicationMapper.toResponse(application, false))
                .build();
    }

    @GetMapping
    public RestResponse<List<ApplicationResponse>> getApplicationsByJob(
            @RequestParam("jobId") String jobId,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (companyId == null || companyId.isBlank()) {
            throw new BadRequestException("Candidate ID invalid");
        }

        List<Application> applications = applicationService.getAllApplications(jobId, companyId);

        return RestResponse.<List<ApplicationResponse>>builder()
                .status(HttpStatus.CREATED)
                .message("Application created successfully")
                .data(applicationMapper.toResponseList(applications, true))
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<ApplicationResponse> getApplicationById(@PathVariable String id) {
        Application application = applicationService.getApplicationById(id);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.OK)
                .message("Application retrieved successfully")
                .data(applicationMapper.toResponse(application, true))
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<ApplicationResponse> updateApplication(@PathVariable String id, @Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.updateApplication(id, request);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.OK)
                .message("Application updated successfully")
                .data(applicationMapper.toResponse(application, false))
                .build();
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteApplication(@PathVariable String id) {
        applicationService.deleteApplication(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Application deleted successfully")
                .build();
    }

    @PutMapping("/{id}/stage")
    public RestResponse<ApplicationResponse> updateApplicationStage(
            @PathVariable String id,
            @Valid @RequestBody ApplicationStageUpdateRequest request,
            Authentication authentication
    ) {
        if (!StringUtils.hasText(id)) {
            throw new BadRequestException("Application ID invalid");
        }

        String changeBy = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(changeBy)) {
            throw new BadRequestException("ID of HR change stage is required");
        }

        request.setChangeBy(changeBy);

        Application application = applicationService.updateApplicationStage(id, request);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.OK)
                .message("Application stage updated successfully")
                .data(applicationMapper.toResponse(application, false))
                .build();
    }
}
