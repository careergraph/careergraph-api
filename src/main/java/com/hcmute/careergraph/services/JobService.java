package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.dtos.request.JobRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobDto createJob(JobRequest request);

    JobDto getJobById(String id);

    Page<JobDto> getAllJobs(Pageable pageable);

    Page<JobDto> getJobsByCompany(String companyId, Pageable pageable);

    JobDto updateJob(String id, JobRequest request);

    void deleteJob(String id);

    void activateJob(String id);

    void deactivateJob(String id);
}
