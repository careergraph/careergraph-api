package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobMapper jobMapper;

    /**
     * Tạo job mới
     *
     * @param request JobCreationRequest từ client
     * @param companyId ID của công ty đăng job (lấy từ authenticated user)
     * @return JobResponse chứa thông tin job vừa tạo
     * @throws IllegalArgumentException nếu company không tồn tại
     */
    @Transactional
    @Override
    public Job createJob(JobCreationRequest request, String companyId) {
        log.info("Creating new job with title: {} for company ID: {}", request.title(), companyId);

        // 1. Validate và lấy Company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + companyId));

        // 3. Map request -> entity
        Job job = jobMapper.toEntity(request, company);

        // 4. Lưu vào database
        Job savedJob = jobRepository.save(job);
        log.info("Job created successfully with ID: {}", savedJob.getId());

        return savedJob;
    }

    /**
     * Lấy thông tin chi tiết job theo ID
     *
     * @param jobId ID của job cần lấy
     * @return JobResponse
     * @throws IllegalArgumentException nếu job không tồn tại
     */
    @Transactional(readOnly = true)
    @Override
    public Job getJobById(String jobId) {
        log.info("Fetching job with ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        return job;
    }

    /**
     * Lấy tất cả jobs của một company
     *
     * @param companyId ID của company
     * @return List JobResponse
     */
    @Transactional(readOnly = true)
    @Override
    public Page<Job> getJobsByCompany(String companyId, Pageable pageable) {
        log.info("Fetching all jobs for company ID: {}", companyId);

        Page<Job> jobs = jobRepository.findByCompanyId(companyId, pageable);
        return jobs;
    }

    @Override
    public Page<Job> getAllJobs(Pageable pageable) {
        log.info("Fetching all jobs");

        Page<Job> jobs = jobRepository.findAll(pageable);
        return jobs;
    }

    /**
     * Update job
     */
    @Transactional
    @Override
    public Job updateJob(String jobId, JobCreationRequest request, String companyId) {
        // TODO: Implement update logic
        throw new UnsupportedOperationException("Update job not implemented yet");
    }

    /**
     * Delete job (soft delete - chuyển status sang CLOSED)
     */
    @Transactional
    @Override
    public void deleteJob(String jobId, String companyId) {
        log.info("Deleting job with ID: {} for company ID: {}", jobId, companyId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        // Validate job thuộc về company
        if (!job.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Job does not belong to this company");
        }

        // Soft delete: chuyển status sang CLOSED
        job.setStatus(Status.CLOSED);
        jobRepository.save(job);

        log.info("Job deleted successfully with ID: {}", jobId);
    }

    @Override
    public void activateJob(String jobId, String companyId) {

    }

    @Override
    public void deactivateJob(String jobId, String companyId) {

    }

    @Override
    public List<HashMap<String, Object>> getJobCategories() {
        return List.of();
    }

    @Override
    public List<Job> getJobsPersonalized(String userId) {
        return List.of();
    }
}
