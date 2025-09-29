package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.response.CompanyDto;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequest;
import com.hcmute.careergraph.services.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public RestResponse<CompanyDto> createCompany(@Valid @RequestBody CompanyRequest request) {
        CompanyDto company = companyService.createCompany(request);
        return RestResponse.<CompanyDto>builder()
                .status(HttpStatus.CREATED)
                .message("Company created successfully")
                .data(company)
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<CompanyDto> getCompanyById(@PathVariable String id) {
        CompanyDto company = companyService.getCompanyById(id);
        return RestResponse.<CompanyDto>builder()
                .status(HttpStatus.OK)
                .message("Company retrieved successfully")
                .data(company)
                .build();
    }

    @GetMapping
    public RestResponse<Page<CompanyDto>> getAllCompanies(Pageable pageable) {
        Page<CompanyDto> companies = companyService.getAllCompanies(pageable);
        return RestResponse.<Page<CompanyDto>>builder()
                .status(HttpStatus.OK)
                .message("Companies retrieved successfully")
                .data(companies)
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<CompanyDto> updateCompany(@PathVariable String id, @Valid @RequestBody CompanyRequest request) {
        CompanyDto company = companyService.updateCompany(id, request);
        return RestResponse.<CompanyDto>builder()
                .status(HttpStatus.OK)
                .message("Company updated successfully")
                .data(company)
                .build();
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteCompany(@PathVariable String id) {
        companyService.deleteCompany(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Company deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/activate")
    public RestResponse<Void> activateCompany(@PathVariable String id) {
        companyService.activateCompany(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Company activated successfully")
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    public RestResponse<Void> deactivateCompany(@PathVariable String id) {
        companyService.deactivateCompany(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Company deactivated successfully")
                .build();
    }
}
