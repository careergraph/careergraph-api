package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.job.JobCategory;
import com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface JobService {

    Job createJob(JobCreationRequest request, String companyId);

    Job getJobById(String jobId);

    Page<Job> getJobsByCompany(String companyId, Pageable pageable);

    Page<Job> getJobByCategory(JobCategory jobCategory, Pageable pageable);

    Page<Job> getAllJobs(Pageable pageable);

    Job updateJob(String jobId, JobCreationRequest request, String companyId);

    Job publishJob(String jobId, String companyId);

    void deleteJob(String jobId, String companyId);

    void activateJob(String jobId, String companyId);

    void deactivateJob(String jobId, String companyId);

    List<Job> getJobsPersonalized(String userId);

    Map<String, String> lookup(String companyId, String query);

    Page<Job> search(JobFilterRequest filter, String companyId, String query, Pageable pageable);
}
