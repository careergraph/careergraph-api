package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.CompanyDto;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    CompanyDto createCompany(CompanyRequest request);

    CompanyDto getCompanyById(String id);

    Page<CompanyDto> getAllCompanies(Pageable pageable);

    CompanyDto updateCompany(String id, CompanyRequest request);

    void deleteCompany(String id);

    void activateCompany(String id);

    void deactivateCompany(String id);
}
