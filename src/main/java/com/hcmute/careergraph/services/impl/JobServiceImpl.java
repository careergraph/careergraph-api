package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.work.JobCategory;
import com.hcmute.careergraph.enums.common.Status;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobMapper jobMapper;

//    @Override
//    public JobDto createJob(JobRequest request) {
//        log.info("Creating new job with title: {}", request.getTitle());
//
//        // Find company
//        Company company = companyRepository.findById(request.getCompanyId())
//                .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
//
//        // Create job entity
//        Job job = Job.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .requirements(request.getRequirements())
//                .benefits(request.getBenefits())
//                .salaryRange(request.getSalaryRange())
//                .experienceLevel(request.getExperienceLevel())
//                .workArrangement(request.getWorkArrangement())
//                .postedDate(request.getPostedDate() != null ? request.getPostedDate() :
//                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
//                .expiryDate(request.getExpiryDate())
//                .numberOfPositions(request.getNumberOfPositions())
//                .workLocation(request.getWorkLocation())
//                .employmentType(request.getEmploymentType())
//                .status(Status.ACTIVE)
//                .isUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : false)
//                .company(company)
//                .build();
//
//        Job savedJob = jobRepository.save(job);
//        log.info("Job created successfully with id: {}", savedJob.getId());
//
//        return convertToDto(job, false);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public JobDto getJobById(String id) {
//        log.info("Getting job by id: {}", id);
//
//        Job job = jobRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
//
//        return convertToDto(job, true);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<JobDto> getAllJobs(Pageable pageable) {
//        log.info("Getting all jobs with pagination");
//        Page<Job> jobPage = jobRepository.findAll(pageable);
//
//        List<JobDto> jobDtos = convertToDto(jobPage, false);
//
//        return new PageImpl<>(jobDtos, pageable, jobDtos.size());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<JobDto> getJobsByCompany(String companyId, Pageable pageable) {
//        log.info("Getting jobs by company id: {}", companyId);
//        Page<Job> jobPage = jobRepository.findByCompanyId(companyId, pageable);
//
//        List<JobDto> jobDtos = convertToDto(jobPage, false);
//
//        return new PageImpl<>(jobDtos, pageable, jobDtos.size());
//    }
//
//    @Override
//    public JobDto updateJob(String id, JobRequest request) {
//        log.info("Updating job with id: {}", id);
//
//        Job job = jobRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
//
//        // Update fields
//        job.setTitle(request.getTitle());
//        job.setDescription(request.getDescription());
//        job.setRequirements(request.getRequirements());
//        job.setBenefits(request.getBenefits());
//        job.setSalaryRange(request.getSalaryRange());
//        job.setExperienceLevel(request.getExperienceLevel());
//        job.setWorkArrangement(request.getWorkArrangement());
//        job.setExpiryDate(request.getExpiryDate());
//        job.setNumberOfPositions(request.getNumberOfPositions());
//        job.setWorkLocation(request.getWorkLocation());
//        job.setEmploymentType(request.getEmploymentType());
//        job.setIsUrgent(request.getIsUrgent());
//
//        // Update company if changed
//        if (!job.getCompany().getId().equals(request.getCompanyId())) {
//            Company company = companyRepository.findById(request.getCompanyId())
//                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
//            job.setCompany(company);
//        }
//
//        Job updatedJob = jobRepository.save(job);
//        log.info("Job updated successfully with id: {}", updatedJob.getId());
//
//        return convertToDto(job, false);
//    }
//
//    @Override
//    public void deleteJob(String id) {
//        log.info("Deleting job with id: {}", id);
//        Job job = jobRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
//        job.softDelete();
//        jobRepository.save(job);
//        log.info("Job soft deleted successfully with id: {}", id);
//    }
//
//    @Override
//    public void activateJob(String id) {
//        log.info("Activating job with id: {}", id);
//        Job job = jobRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
//        job.activate();
//        jobRepository.save(job);
//        log.info("Job activated successfully with id: {}", id);
//    }
//
//    @Override
//    public void deactivateJob(String id) {
//        log.info("Deactivating job with id: {}", id);
//        Job job = jobRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
//        job.deactivate();
//        jobRepository.save(job);
//        log.info("Job deactivated successfully with id: {}", id);
//    }

    @Override
    public JobDto createJob(JobRequest request) {
        return null;
    }

    @Override
    public JobDto getJobById(String id) {
        return null;
    }

    @Override
    public Page<JobDto> getAllJobs(Pageable pageable) {
        return null;
    }

    @Override
    public Page<JobDto> getJobsByCompany(String companyId, Pageable pageable) {
        return null;
    }

    @Override
    public JobDto updateJob(String id, JobRequest request) {
        return null;
    }

    @Override
    public void deleteJob(String id) {

    }

    @Override
    public void activateJob(String id) {

    }

    @Override
    public void deactivateJob(String id) {

    }

    @Override
    public List<HashMap<String, Object>> getJobCategories() {

        List<HashMap<String, Object>> result = new ArrayList<>();
        List<JobCategory> jobCategories = List.of(JobCategory.values());
        jobCategories.stream().map(category -> {
            HashMap<String, Object> tmp = new HashMap<>();
            tmp.put("type", category.name());
            tmp.put("name", category.getDisplayName());
            tmp.put("description", category.getDescription());

            result.add(tmp);
            return tmp;
        }).toList();

        return result;
    }

    @Override
    public Page<JobDto> getJobsPersonalized(Pageable pageable) {
        return null;
    }

    // ========================================= CONVERT FUNC =========================================

    /*
    * Convert DTO from JobPage to JobList
    * */
    private List<JobDto> convertToDto(Page<Job> jobPage, boolean isDetail) {
        List<JobDto> result = jobPage.stream()
                .filter(job -> job.getStatus() == Status.ACTIVE)
                .map(job -> {
                    JobDto tmp = jobMapper.toDto(job);

                    // Build category
                    HashMap<String, Object> jobCategory = new HashMap<>();
                    jobCategory.put("type", job.getJobCategory().name());
                    jobCategory.put("name", job.getJobCategory().getDisplayName());
                    jobCategory.put("description", job.getJobCategory().getDescription());
                    // tmp.setJobCategory(jobCategory);

                    // Build detail
                    if (isDetail) {
                        putDetail(tmp);
                    }

                    return tmp;
                })
                .toList();

        return result;
    }

    /*
    * Convert DTO from list
    * */
    private List<JobDto> convertToDto(List<Job> jobs, boolean isDetail) {
        List<JobDto> result = jobs.stream()
                .filter(job -> job.getStatus() == Status.ACTIVE)
                .map(job -> {
                    JobDto tmp = jobMapper.toDto(job);

                    // Build category
                    HashMap<String, Object> jobCategory = new HashMap<>();
                    jobCategory.put("type", job.getJobCategory().name());
                    jobCategory.put("name", job.getJobCategory().getDisplayName());
                    jobCategory.put("description", job.getJobCategory().getDescription());
                    // tmp.setJobCategory(jobCategory);

                    // Build detail
                    if (isDetail) {
                        putDetail(tmp);
                    }

                    return tmp;
                })
                .toList();

        return result;
    }

    /*
    * Convert DTO from single record
    * */
    private JobDto convertToDto(Job job, boolean isDetail) {
        JobDto result = jobMapper.toDto(job);

        // Build category
        HashMap<String, Object> jobCategory = new HashMap<>();
        jobCategory.put("type", job.getJobCategory().name());
        jobCategory.put("name", job.getJobCategory().getDisplayName());
        jobCategory.put("description", job.getJobCategory().getDescription());
        // result.setJobCategory(jobCategory);

        // Build detail
        if (isDetail) {
            putDetail(result);
        }

        return result;
    }

    /*
    * Convert detail
    * */
    private void putDetail(JobDto jobDto) {
        // TODO: application and required skill
    }
}
