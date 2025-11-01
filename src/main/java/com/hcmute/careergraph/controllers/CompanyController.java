package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CompanyMapper;
import com.hcmute.careergraph.persistence.dtos.response.CompanyResponse;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final SecurityUtils securityUtils;
    private final CompanyMapper companyMapper;

    @GetMapping("/me")
    public RestResponse<CompanyResponse> getCompanyProfile(Authentication authentication) {
        Company company = companyService.getCompanyById(securityUtils.extractCompanyId(authentication));

        return RestResponse.<CompanyResponse>builder()
                .status(HttpStatus.OK)
                .data(companyMapper.toResponse(company))
                .build();
    }
}
