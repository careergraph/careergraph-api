package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.ApplicationDto;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    ApplicationDto createApplication(ApplicationRequest request);

    ApplicationDto getApplicationById(String id);

    Page<ApplicationDto> getAllApplications(Pageable pageable);

    Page<ApplicationDto> getApplicationsByCandidate(String candidateId, Pageable pageable);

    Page<ApplicationDto> getApplicationsByJob(String jobId, Pageable pageable);

    ApplicationDto updateApplication(String id, ApplicationRequest request);

    void deleteApplication(String id);

    void updateApplicationStatus(String id, String status);
}
