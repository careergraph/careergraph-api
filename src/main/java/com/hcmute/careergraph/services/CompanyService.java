package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Company;

public interface CompanyService {

    Company getCompanyById(String companyId);
}
