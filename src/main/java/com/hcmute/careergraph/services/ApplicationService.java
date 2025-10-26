package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationStageUpdateRequest;
import com.hcmute.careergraph.persistence.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    Application createApplication(ApplicationRequest request);

    Application getApplicationById(String id);

    Page<Application> getAllApplications(Pageable pageable);

    Page<Application> getApplicationsByCandidate(String candidateId, Pageable pageable);

    Page<Application> getApplicationsByJob(String jobId, Pageable pageable);

    Application updateApplication(String id, ApplicationRequest request);

    void deleteApplication(String id);

    Application updateApplicationStage(String id, ApplicationStageUpdateRequest request);
}
