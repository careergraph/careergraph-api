package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.CompanyRequest;
import com.hcmute.careergraph.persistence.models.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    Company createCompany(CompanyRequest request);

    Company getCompanyById(String id);

    Page<Company> getAllCompanies(Pageable pageable);

    Company updateCompany(String id, CompanyRequest request);

    void deleteCompany(String id);

    void activateCompany(String id);

    void deactivateCompany(String id);
}
