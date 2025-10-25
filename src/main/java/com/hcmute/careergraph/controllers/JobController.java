package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.record.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.record.JobResponse;
import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.dtos.request.JobRequest;
import com.hcmute.careergraph.services.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/jobs
     * Tạo job mới
     *
     * @param request JobCreationRequest với validation
     * @param authentication Spring Security Authentication (để lấy company ID)
     * @return RestResponse<JobResponse>
     */
    @PostMapping
    public RestResponse<JobResponse> createJob(
            @Valid @RequestBody JobCreationRequest request,
            Authentication authentication
    ) {
        log.info("POST /api/v1/jobs - Creating job: {}", request.title());

        // Extract company ID from authenticated user
        String companyId = extractCompanyId(authentication);

        // Delegate to service
        JobResponse job = jobService.createJob(request, companyId);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Job created successfully")
                .data(job)
                .build();
    }

    /**
     * GET /api/v1/jobs/{id}
     * Lấy thông tin job theo ID
     *
     * @param id Job ID
     * @return RestResponse<JobResponse>
     */
    @GetMapping("/{id}")
    public RestResponse<JobResponse> getJobById(@PathVariable String id) {
        log.info("GET /api/v1/jobs/{} - Fetching job", id);

        JobResponse job = jobService.getJobById(id);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Job retrieved successfully")
                .data(job)
                .build();
    }

    /**
     * GET /api/v1/jobs
     * Lấy tất cả jobs với pagination
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return RestResponse<Page<JobResponse>>
     */
    @GetMapping
    public RestResponse<Page<JobResponse>> getAllJobs(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size
    ) {
        log.info("GET /api/v1/jobs - Fetching all jobs (page: {}, size: {})", page, size);

        Pageable pageable = PageRequest.of(page, size);

        // TODO: Cần update JobService để support pagination
        List<JobResponse> allJobs = jobService.getAllJobs();

        // Temporary: Convert List to Page (nên implement proper pagination trong service)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allJobs.size());
        List<JobResponse> pageContent = allJobs.subList(start, end);
        Page<JobResponse> jobs = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, allJobs.size()
        );

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(jobs)
                .build();
    }

    /**
     * GET /api/v1/jobs/company/{companyId}
     * Lấy tất cả jobs của một company với pagination
     *
     * @param companyId Company ID
     * @param page Page number
     * @param size Page size
     * @return RestResponse<Page<JobResponse>>
     */
    @GetMapping("/company/{companyId}")
    public RestResponse<Page<JobResponse>> getJobsByCompany(
            @PathVariable String companyId,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size
    ) {
        log.info("GET /api/v1/jobs/company/{} - Fetching jobs (page: {}, size: {})",
                companyId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        // Get jobs from service
        List<JobResponse> allJobs = jobService.getJobsByCompany(companyId);

        // Convert to Page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allJobs.size());
        List<JobResponse> pageContent = allJobs.subList(start, end);
        Page<JobResponse> jobs = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, allJobs.size()
        );

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(jobs)
                .build();
    }

    /**
     * PUT /api/v1/jobs/{id}
     * Cập nhật job
     *
     * @param id Job ID
     * @param request JobCreationRequest với data mới
     * @param authentication Authentication để lấy company ID
     * @return RestResponse<JobResponse>
     */
    @PutMapping("/{id}")
    public RestResponse<JobResponse> updateJob(
            @PathVariable String id,
            @Valid @RequestBody JobCreationRequest request,
            Authentication authentication
    ) {
        log.info("PUT /api/v1/jobs/{} - Updating job", id);

        String companyId = extractCompanyId(authentication);

        JobResponse job = jobService.updateJob(id, request, companyId);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Job updated successfully")
                .data(job)
                .build();
    }

    /**
     * DELETE /api/v1/jobs/{id}
     * Xóa job (soft delete)
     *
     * @param id Job ID
     * @param authentication Authentication để lấy company ID
     * @return RestResponse<Void>
     */
    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("DELETE /api/v1/jobs/{} - Deleting job", id);

        String companyId = extractCompanyId(authentication);

        jobService.deleteJob(id, companyId);

        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Job deleted successfully")
                .build();
    }

    /**
     * PATCH /api/v1/jobs/{id}/activate
     * Kích hoạt job (set status = ACTIVE)
     *
     * @param id Job ID
     * @param authentication Authentication để lấy company ID
     * @return RestResponse<Void>
     */
    @PatchMapping("/{id}/activate")
    public RestResponse<Void> activateJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("PATCH /api/v1/jobs/{}/activate - Activating job", id);

        String companyId = extractCompanyId(authentication);

        // TODO: Cần thêm method activateJob trong JobService
        // jobService.activateJob(id, companyId);

        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Job activated successfully")
                .build();
    }

    /**
     * PATCH /api/v1/jobs/{id}/deactivate
     * Vô hiệu hóa job (set status = INACTIVE)
     *
     * @param id Job ID
     * @param authentication Authentication để lấy company ID
     * @return RestResponse<Void>
     */
    @PatchMapping("/{id}/deactivate")
    public RestResponse<Void> deactivateJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("PATCH /api/v1/jobs/{}/deactivate - Deactivating job", id);

        String companyId = extractCompanyId(authentication);

        // TODO: Cần thêm method deactivateJob trong JobService
        // jobService.deactivateJob(id, companyId);

        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Job deactivated successfully")
                .build();
    }

    /**
     * GET /api/v1/jobs/categories
     * Lấy danh sách job categories
     *
     * @return RestResponse<List<HashMap<String, Object>>>
     */
    @GetMapping("/categories")
    public RestResponse<List<HashMap<String, Object>>> getJobCategories() {
        log.info("GET /api/v1/jobs/categories - Fetching job categories");

        // TODO: Cần thêm method getJobCategories trong JobService
        // List<HashMap<String, Object>> categories = jobService.getJobCategories();

        return RestResponse.<List<HashMap<String, Object>>>builder()
                .status(HttpStatus.OK)
                .message("List of job category")
                .data(null) // TODO: Replace with actual data
                .build();
    }

    /**
     * GET /api/v1/jobs/personalized
     * Lấy danh sách jobs được personalized cho user hiện tại
     * Dựa trên skills, experience, preferences của candidate
     *
     * @param page Page number
     * @param size Page size
     * @param authentication Authentication để lấy candidate info
     * @return RestResponse<Page<JobResponse>>
     */
    @GetMapping("/personalized")
    public RestResponse<Page<JobResponse>> getJobsPersonalized(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            Authentication authentication
    ) {
        log.info("GET /api/v1/jobs/personalized - Fetching personalized jobs");

        Pageable pageable = PageRequest.of(page, size);

        // TODO: Cần thêm method getJobsPersonalized trong JobService
        // String candidateId = extractCandidateId(authentication);
        // Page<JobResponse> jobs = jobService.getJobsPersonalized(candidateId, pageable);

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(null) // TODO: Replace with actual data
                .build();
    }

    /**
     * Helper method: Extract company ID từ Authentication
     *
     * @param authentication Spring Security Authentication
     * @return Company ID
     */
    private String extractCompanyId(Authentication authentication) {

        // Extract from JWT claims
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        if (jwt != null) {
            return jwt.getToken().getClaimAsString("companyId");
        }
        log.error("Not found jwt token");
        return null;
    }

    /**
     * Helper method: Extract candidate ID từ Authentication
     * Dùng cho personalized jobs
     *
     * @param authentication Spring Security Authentication
     * @return Candidate ID
     */
    private String extractCandidateId(Authentication authentication) {
        // Extract from JWT claims
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        if (jwt != null) {
            return jwt.getToken().getClaimAsString("candidateId");
        }
        log.error("Not found jwt token");
        return null;
    }
}
