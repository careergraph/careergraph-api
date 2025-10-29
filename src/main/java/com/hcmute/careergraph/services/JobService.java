package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest;
import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;

public interface JobService {

    Job createJob(JobCreationRequest request, String companyId);

    Job getJobById(String jobId);

    Page<Job> getJobsByCompany(String companyId, Pageable pageable);

    Page<Job> getAllJobs(Pageable pageable);

    Job updateJob(String jobId, JobCreationRequest request, String companyId);

    Job publishJob(String jobId, String companyId);

    void deleteJob(String jobId, String companyId);

    void activateJob(String jobId, String companyId);

    void deactivateJob(String jobId, String companyId);

    List<HashMap<String, Object>> getJobCategories();

    List<Job> getJobsPersonalized(String userId);
}
