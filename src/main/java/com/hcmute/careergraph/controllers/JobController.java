package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.enums.job.JobCategory;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobRecruimentRequest;
import com.hcmute.careergraph.persistence.dtos.response.JobResponse;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.services.ApplicationService;
import com.hcmute.careergraph.services.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final JobMapper jobMapper;
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
        String companyId = securityUtils.extractCompanyId(authentication);

        // Delegate to service
        Job job = jobService.createJob(request, companyId);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Job created successfully")
                .data(jobMapper.toResponse(job))
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

        Job job = jobService.getJobById(id);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Job retrieved successfully")
                .data(jobMapper.toResponse(job))
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

        Page<Job> jobPage = jobService.getAllJobs(pageable);

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(mapToJobResponsePage(jobPage, pageable))
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

        String companyId = securityUtils.extractCompanyId(authentication);

        Job job = jobService.updateJob(id, request, companyId);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Job updated successfully")
                .data(jobMapper.toResponse(job))
                .build();
    }

    @PutMapping("/{id}/recruitment")
    public RestResponse<JobResponse> updateJobRecruitment(
            @PathVariable("id") String jobId,
            @RequestBody JobRecruimentRequest request,
            Authentication authentication
    ) {
        if (jobId == null) {
            throw new BadRequestException("Job ID is required");
        }
        String companyId = securityUtils.extractCompanyId(authentication);

        Job job = jobService.updateJob(jobId, companyId, request);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Job updated successfully")
                .data(jobMapper.toResponse(job))
                .build();
    }

    @PostMapping("/{id}/apply")
    public RestResponse<JobResponse> applyJob(
            @PathVariable("id") String jobId,
            @RequestBody JobRecruimentRequest request,
            Authentication authentication) {
        if (jobId == null) {
            throw new BadRequestException("Job ID is required");
        }
        String candidateId = securityUtils.extractCandidateId(authentication);



        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Apply job successfully")
                .data(jobMapper.toResponse(null))
                .build();
    }


    /**
     * PUT /api/v1/jobs/{id}/publish
     * Cập nhật job from DRAFT to ACTIVE
     *
     * @param id Job ID
     * @param authentication Authentication để lấy company ID
     * @return RestResponse<JobResponse>
     */
    @PutMapping("/{id}/publish")
    public RestResponse<JobResponse> publishJob(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("PUT /api/v1/jobs/{} - Updating job", id);

        String companyId = securityUtils.extractCompanyId(authentication);

        Job job = jobService.publishJob(id, companyId);

        return RestResponse.<JobResponse>builder()
                .status(HttpStatus.OK)
                .message("Job publish successfully")
                .data(jobMapper.toResponse(job))
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

        String companyId = securityUtils.extractCompanyId(authentication);

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

        String companyId = securityUtils.extractCompanyId(authentication);

        jobService.activateJob(id, companyId);

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

        String companyId = securityUtils.extractCompanyId(authentication);

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
    public RestResponse<List<JobResponse>> getJobsPersonalized(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            Authentication authentication
    ) {
        log.info("GET /api/v1/jobs/personalized - Fetching personalized jobs");

        Pageable pageable = PageRequest.of(page, size);

        String candidateId = securityUtils.extractCandidateId(authentication);
        List<Job> recommendationJobs = new ArrayList<>();
        if (candidateId == null) {
            recommendationJobs = jobService.getJobsForAnonymousUser();
        } else {
            recommendationJobs = jobService.getJobsPersonalized(candidateId);
        }

        return RestResponse.<List<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(mapToJobResponseList(recommendationJobs))
                .build();
    }

    /**
     * GET /api/v1/jobs/popular
     * Lấy danh sách jobs pho bien
     * Dựa trên skills, experience, preferences của candidate
     *
     * @param page Page number
     * @param size Page size
     * @param authentication Authentication để lấy candidate info
     * @return RestResponse<Page<JobResponse>>
     */
    @GetMapping("/popular")
    public RestResponse<List<JobResponse>> getJobsPopular(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            Authentication authentication
    ) {
        log.info("GET /api/v1/jobs/popular - Fetching personalized jobs");

        Pageable pageable = PageRequest.of(page, size);

        List<Job> jobsPopular = jobService.getJobsPopular();

        return RestResponse.<List<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(mapToJobResponseList(jobsPopular))
                .build();
    }

    /**
     * GET /api/v1/jobs/category
     * Lấy danh sách jobs theo category of all company
     *
     * @param page Page number
     * @param size Page size
     * @param authentication Authentication để lấy candidate info
     * @return RestResponse<Page<JobResponse>>
     */
    @GetMapping("/category")
    public RestResponse<Page<JobResponse>> getJobsByCategory(
            @RequestParam(name = "category", defaultValue = "") JobCategory jobCategory,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            Authentication authentication
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Job> jobPage = jobService.getJobByCategory(jobCategory, pageable);

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(mapToJobResponsePage(jobPage, pageable))
                .build();
    }

    /**
     * GET /api/v1/jobs/lookup
     * Lấy danh sách jobs được theo query truyền vào
     * Lấy dựa trên company ID
     *
     * @param authentication Authentication để lấy candidate info
     * @param query Query truyền vào để truy vấn
     * @return RestResponse<Map<String, String>>
     */
    @GetMapping("/lookup")
    public RestResponse<Map<String, String>> lookup(@RequestParam(required = false) String query,
                                                    Authentication authentication) {
        log.info("GET /api/v1/jobs/lookup - Fetching lookup jobs");

        String companyId = securityUtils.extractCompanyId(authentication);
        if (companyId == null) {
            throw new BadRequestException("Company ID is not null");
        }

        Map<String, String> jobs = jobService.lookup(companyId, query);

        return RestResponse.<Map<String, String>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(jobs)
                .build();
    }

    /**
     * POST /api/v1/jobs/search
     * Lấy danh sách jobs được theo query truyền vào và filter
     * Lấy dựa trên company ID
     *
     * @param authentication Authentication để lấy candidate info
     * @param query Query truyền vào để truy vấn
     * @return RestResponse<Page<JobResponse>>
     */
    @PostMapping("/search")
    public RestResponse<Page<JobResponse>> search(
            @RequestBody JobFilterRequest filter,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "") String query,
            Authentication authentication) {
        log.info("POST /api/v1/jobs/search - Fetching lookup jobs");

        String companyId = securityUtils.extractCompanyId(authentication);
        String candidateId = securityUtils.extractCandidateId(authentication);

        Pageable pageable = PageRequest.of(page, size);
        Page<Job> jobPage = (companyId != null && !companyId.isEmpty())
                ? jobService.search(filter, companyId, query, pageable, PartyType.COMPANY)
                : jobService.search(filter, candidateId, query, pageable, PartyType.CANDIDATE);

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .message("Jobs retrieved successfully")
                .data(mapToJobResponsePage(jobPage, pageable))
                .build();
    }

    // Helper method to map to response page
    private Page<JobResponse> mapToJobResponsePage(Page<Job> jobPage, Pageable pageable) {
        List<JobResponse> jobResponses = jobPage.stream()
                .map(job -> jobMapper.toResponse(job))
                .toList();
        return new PageImpl<>(jobResponses, pageable, jobPage.getTotalElements());
    }

    // Helper method to map to response page
    private List<JobResponse> mapToJobResponseList(List<Job> jobs) {
        List<JobResponse> jobResponses = jobs.stream()
                .map(job -> jobMapper.toResponse(job))
                .toList();
        return jobResponses;
    }
}
