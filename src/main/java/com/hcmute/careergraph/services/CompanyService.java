package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Company;

import java.util.HashMap;
import java.util.List;

public interface CompanyService {

    Company getCompanyById(String companyId);
    List<HashMap<String, String>> lookup(String companyId);
}
