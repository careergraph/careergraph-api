package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.dtos.request.JobRequest;
import com.hcmute.careergraph.services.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public RestResponse<JobDto> createJob(@Valid @RequestBody JobRequest request) {
        JobDto job = jobService.createJob(request);
        return RestResponse.<JobDto>builder()
                .status(HttpStatus.CREATED)
                .message("Job created successfully")
                .data(job)
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<JobDto> getJobById(@PathVariable String id) {
        JobDto job = jobService.getJobById(id);
        return RestResponse.<JobDto>builder()
                .status(HttpStatus.OK)
                .message("Job retrieved successfully")
                .data(job)
                .build();
    }

    @GetMapping
    public RestResponse<Page<JobDto>> getAllJobs(Pageable pageable) {
        Page<JobDto> jobs = jobService.getAllJobs(pageable);
        return RestResponse.<Page<JobDto>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(jobs)
                .build();
    }

    @GetMapping("/company/{companyId}")
    public RestResponse<Page<JobDto>> getJobsByCompany(@PathVariable String companyId, Pageable pageable) {
        Page<JobDto> jobs = jobService.getJobsByCompany(companyId, pageable);
        return RestResponse.<Page<JobDto>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(jobs)
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<JobDto> updateJob(@PathVariable String id, @Valid @RequestBody JobRequest request) {
        JobDto job = jobService.updateJob(id, request);
        return RestResponse.<JobDto>builder()
                .status(HttpStatus.OK)
                .message("Job updated successfully")
                .data(job)
                .build();
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteJob(@PathVariable String id) {
        jobService.deleteJob(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Job deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/activate")
    public RestResponse<Void> activateJob(@PathVariable String id) {
        jobService.activateJob(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Job activated successfully")
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    public RestResponse<Void> deactivateJob(@PathVariable String id) {
        jobService.deactivateJob(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Job deactivated successfully")
                .build();
    }
}
