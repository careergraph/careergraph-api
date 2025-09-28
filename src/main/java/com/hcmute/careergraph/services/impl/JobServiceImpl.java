package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.Status;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.dtos.request.JobRequest;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobMapper jobMapper;

    @Override
    public JobDto createJob(JobRequest request) {
        log.info("Creating new job with title: {}", request.getTitle());
        
        // Find company
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));

        // Create job entity
        Job job = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .salaryRange(request.getSalaryRange())
                .experienceLevel(request.getExperienceLevel())
                .workArrangement(request.getWorkArrangement())
                .postedDate(request.getPostedDate() != null ? request.getPostedDate() : 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .expiryDate(request.getExpiryDate())
                .numberOfPositions(request.getNumberOfPositions())
                .workLocation(request.getWorkLocation())
                .employmentType(request.getEmploymentType())
                .status(Status.ACTIVE)
                .isUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : false)
                .company(company)
                .build();

        Job savedJob = jobRepository.save(job);
        log.info("Job created successfully with id: {}", savedJob.getId());
        
        return jobMapper.toDto(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDto getJobById(String id) {
        log.info("Getting job by id: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        return jobMapper.toDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> getAllJobs(Pageable pageable) {
        log.info("Getting all jobs with pagination");
        Page<Job> jobs = jobRepository.findAll(pageable);
        return jobs.map(jobMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> getJobsByCompany(String companyId, Pageable pageable) {
        log.info("Getting jobs by company id: {}", companyId);
        Page<Job> jobs = jobRepository.findByCompanyId(companyId, pageable);
        return jobs.map(jobMapper::toDto);
    }

    @Override
    public JobDto updateJob(String id, JobRequest request) {
        log.info("Updating job with id: {}", id);
        
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));

        // Update fields
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setBenefits(request.getBenefits());
        job.setSalaryRange(request.getSalaryRange());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setWorkArrangement(request.getWorkArrangement());
        job.setExpiryDate(request.getExpiryDate());
        job.setNumberOfPositions(request.getNumberOfPositions());
        job.setWorkLocation(request.getWorkLocation());
        job.setEmploymentType(request.getEmploymentType());
        job.setIsUrgent(request.getIsUrgent());

        // Update company if changed
        if (!job.getCompany().getId().equals(request.getCompanyId())) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
            job.setCompany(company);
        }

        Job updatedJob = jobRepository.save(job);
        log.info("Job updated successfully with id: {}", updatedJob.getId());
        
        return jobMapper.toDto(updatedJob);
    }

    @Override
    public void deleteJob(String id) {
        log.info("Deleting job with id: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        job.softDelete();
        jobRepository.save(job);
        log.info("Job soft deleted successfully with id: {}", id);
    }

    @Override
    public void activateJob(String id) {
        log.info("Activating job with id: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        job.activate();
        jobRepository.save(job);
        log.info("Job activated successfully with id: {}", id);
    }

    @Override
    public void deactivateJob(String id) {
        log.info("Deactivating job with id: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        job.deactivate();
        jobRepository.save(job);
        log.info("Job deactivated successfully with id: {}", id);
    }
}
