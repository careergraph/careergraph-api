package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequests;

import java.util.HashMap;
import java.util.List;

public interface CompanyService {

    Company getCompanyById(String companyId);

    Company updateMyProfile(String companyId, CompanyRequests.UpdateMyProfileRequest request);

    List<HashMap<String, String>> lookup(String companyId);
}
