package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationStageUpdateRequest;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {

    Application createApplication(ApplicationRequest request);

    Application getApplicationById(String id);

    List<Application> getAllApplications(String jobId, String companyId);

    Page<Application> getApplicationsByCandidate(String candidateId, Pageable pageable);

    Page<Application> getApplicationsByJob(String jobId, Pageable pageable);

    Application updateApplication(String id, ApplicationRequest request);

    void deleteApplication(String id);

    Application updateApplicationStage(String id, ApplicationStageUpdateRequest request);

    /**
     * Khi HR chủ động nhắn tin, tự chuyển hồ sơ từ APPLIED/SCREENING sang HR_CONTACTED.
     */
    void promoteToHrContactedOnHrMessage(String applicationId, Account hrAccount);

    Page<Application> getApplicationsByCandidateWithJob(String candidateId, Pageable pageable);
    Page<Application> getApplicationsByCandidateWithJobWithStatus(String candidateId, Pageable pageable, ApplicationStage status);
    boolean existsApplicationsByJobIdAndCandidateId(String jobId, String candidateId);

    /**
     * Trả về true khi ứng viên đã có hồ sơ và không được phép ứng tuyển lại
     * (stage không thuộc APPLIED, REJECTED, OFFBOARDED).
     */
    boolean isApplicationReapplyBlocked(String jobId, String candidateId);
}
