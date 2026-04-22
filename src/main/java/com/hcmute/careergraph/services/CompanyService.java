package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyResponse;

import java.util.HashMap;
import java.util.List;

public interface CompanyService {

    Company getCompanyById(String companyId);

    Company updateMyProfile(String companyId, CompanyRequests.UpdateMyProfileRequest request);

    boolean isCandidateFollowingCompany(String candidateId, String companyId);

    boolean toggleCandidateFollowCompany(String candidateId, String companyId);

    List<CompanyResponse> getFollowedCompanies(String candidateId);

    List<HashMap<String, String>> lookup(String companyId);
}
