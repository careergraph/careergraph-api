package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.record.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.record.JobResponse;
import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.dtos.request.JobRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;

public interface JobService {

    JobResponse createJob(JobCreationRequest request, String companyId);

    JobResponse getJobById(String jobId);

    List<JobResponse> getJobsByCompany(String companyId);

    List<JobResponse> getAllJobs();

    JobResponse updateJob(String jobId, JobCreationRequest request, String companyId);

    void deleteJob(String jobId, String companyId);

    void activateJob(String jobId);

    void deactivateJob(String jobId);

    List<HashMap<String, Object>> getJobCategories();

    List<JobResponse> getJobsPersonalized(String userId);
}
