package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.response.ApplicationDto;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
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

    @PostMapping
    public RestResponse<ApplicationDto> createApplication(@Valid @RequestBody ApplicationRequest request) {
        ApplicationDto application = applicationService.createApplication(request);
        return RestResponse.<ApplicationDto>builder()
                .status(HttpStatus.CREATED)
                .message("Application created successfully")
                .data(application)
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<ApplicationDto> getApplicationById(@PathVariable String id) {
        ApplicationDto application = applicationService.getApplicationById(id);
        return RestResponse.<ApplicationDto>builder()
                .status(HttpStatus.OK)
                .message("Application retrieved successfully")
                .data(application)
                .build();
    }

    @GetMapping
    public RestResponse<Page<ApplicationDto>> getAllApplications(Pageable pageable) {
        Page<ApplicationDto> applications = applicationService.getAllApplications(pageable);
        return RestResponse.<Page<ApplicationDto>>builder()
                .status(HttpStatus.OK)
                .message("Applications retrieved successfully")
                .data(applications)
                .build();
    }

    @GetMapping("/candidate/{candidateId}")
    public RestResponse<Page<ApplicationDto>> getApplicationsByCandidate(@PathVariable String candidateId, Pageable pageable) {
        Page<ApplicationDto> applications = applicationService.getApplicationsByCandidate(candidateId, pageable);
        return RestResponse.<Page<ApplicationDto>>builder()
                .status(HttpStatus.OK)
                .message("Applications retrieved successfully")
                .data(applications)
                .build();
    }

    @GetMapping("/job/{jobId}")
    public RestResponse<Page<ApplicationDto>> getApplicationsByJob(@PathVariable String jobId, Pageable pageable) {
        Page<ApplicationDto> applications = applicationService.getApplicationsByJob(jobId, pageable);
        return RestResponse.<Page<ApplicationDto>>builder()
                .status(HttpStatus.OK)
                .message("Applications retrieved successfully")
                .data(applications)
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<ApplicationDto> updateApplication(@PathVariable String id, @Valid @RequestBody ApplicationRequest request) {
        ApplicationDto application = applicationService.updateApplication(id, request);
        return RestResponse.<ApplicationDto>builder()
                .status(HttpStatus.OK)
                .message("Application updated successfully")
                .data(application)
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
