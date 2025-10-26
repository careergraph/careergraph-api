package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.mapper.ApplicationMapper;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.dtos.response.ApplicationResponse;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.services.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;

    @PostMapping
    public RestResponse<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.createApplication(request);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Application created successfully")
                .data(applicationMapper.toResponse(application))
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<ApplicationResponse> getApplicationById(@PathVariable String id) {
        Application application = applicationService.getApplicationById(id);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.OK)
                .message("Application retrieved successfully")
                .data(applicationMapper.toResponse(application))
                .build();
    }

    @GetMapping
    public RestResponse<Page<ApplicationResponse>> getAllApplications(Pageable pageable) {
        Page<Application> applications = applicationService.getAllApplications(pageable);
        return RestResponse.<Page<ApplicationResponse>>builder()
                .status(HttpStatus.OK)
                .message("Applications retrieved successfully")
                .data(applications.map(applicationMapper::toResponse))
                .build();
    }

    @GetMapping("/candidate/{candidateId}")
    public RestResponse<Page<ApplicationResponse>> getApplicationsByCandidate(@PathVariable String candidateId, Pageable pageable) {
        Page<Application> applications = applicationService.getApplicationsByCandidate(candidateId, pageable);
        return RestResponse.<Page<ApplicationResponse>>builder()
                .status(HttpStatus.OK)
                .message("Applications retrieved successfully")
                .data(applications.map(applicationMapper::toResponse))
                .build();
    }

    @GetMapping("/job/{jobId}")
    public RestResponse<Page<ApplicationResponse>> getApplicationsByJob(@PathVariable String jobId, Pageable pageable) {
        Page<Application> applications = applicationService.getApplicationsByJob(jobId, pageable);
        return RestResponse.<Page<ApplicationResponse>>builder()
                .status(HttpStatus.OK)
                .message("Applications retrieved successfully")
                .data(applications.map(applicationMapper::toResponse))
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<ApplicationResponse> updateApplication(@PathVariable String id, @Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.updateApplication(id, request);
        return RestResponse.<ApplicationResponse>builder()
                .status(HttpStatus.OK)
                .message("Application updated successfully")
                .data(applicationMapper.toResponse(application))
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

    @PatchMapping("/{id}/status")
    public RestResponse<Void> updateApplicationStatus(@PathVariable String id, @RequestParam String status) {
        applicationService.updateApplicationStatus(id, status);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Application status updated successfully")
                .build();
    }
}
