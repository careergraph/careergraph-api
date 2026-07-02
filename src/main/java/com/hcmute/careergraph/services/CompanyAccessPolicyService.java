package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;

public interface CompanyAccessPolicyService {

    void assertCurrentAccountIsAdmin();

    void assertCompanyCanManageJobs(Company company);

    void assertCompanyCanSearchCandidates(Company company);

    void assertJobAcceptingCandidateApplications(Job job);

    boolean isJobPubliclyAvailable(Job job);

    boolean isJobVisibleForDetail(Job job);

    boolean isJobExpired(Job job);
}
