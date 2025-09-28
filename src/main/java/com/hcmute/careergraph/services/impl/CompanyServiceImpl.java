package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.mapper.CompanyMapper;
import com.hcmute.careergraph.persistence.dtos.response.CompanyDto;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequest;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyDto createCompany(CompanyRequest request) {
        log.info("Creating new company with tagname: {}", request.getTagname());
        
        Company company = Company.builder()
                .tagname(request.getTagname())
                .avatar(request.getAvatar())
                .cover(request.getCover())
                .size(request.getSize())
                .website(request.getWebsite())
                .ceoName(request.getCeoName())
                .noOfMembers(request.getNoOfMembers() != null ? request.getNoOfMembers() : 0)
                .yearFounded(request.getYearFounded())
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("Company created successfully with id: {}", savedCompany.getId());
        
        return companyMapper.toDto(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(String id) {
        log.info("Getting company by id: {}", id);
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        return companyMapper.toDto(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyDto> getAllCompanies(Pageable pageable) {
        log.info("Getting all companies with pagination");
        Page<Company> companies = companyRepository.findAll(pageable);
        return companies.map(companyMapper::toDto);
    }

    @Override
    public CompanyDto updateCompany(String id, CompanyRequest request) {
        log.info("Updating company with id: {}", id);
        
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));

        // Update fields
        company.setTagname(request.getTagname());
        company.setAvatar(request.getAvatar());
        company.setCover(request.getCover());
        company.setSize(request.getSize());
        company.setWebsite(request.getWebsite());
        company.setCeoName(request.getCeoName());
        company.setNoOfMembers(request.getNoOfMembers() != null ? request.getNoOfMembers() : company.getNoOfMembers());
        company.setYearFounded(request.getYearFounded());

        Company updatedCompany = companyRepository.save(company);
        log.info("Company updated successfully with id: {}", updatedCompany.getId());
        
        return companyMapper.toDto(updatedCompany);
    }

    @Override
    public void deleteCompany(String id) {
        log.info("Deleting company with id: {}", id);
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        company.softDelete();
        companyRepository.save(company);
        log.info("Company soft deleted successfully with id: {}", id);
    }

    @Override
    public void activateCompany(String id) {
        log.info("Activating company with id: {}", id);
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        company.activate();
        companyRepository.save(company);
        log.info("Company activated successfully with id: {}", id);
    }

    @Override
    public void deactivateCompany(String id) {
        log.info("Deactivating company with id: {}", id);
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        company.deactivate();
        companyRepository.save(company);
        log.info("Company deactivated successfully with id: {}", id);
    }
}
