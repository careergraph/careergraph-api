package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CompanyMapper;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.dtos.response.CompanyResponse;
import com.hcmute.careergraph.persistence.dtos.response.JobResponse;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.services.CompanyService;
import com.hcmute.careergraph.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final JobService jobService;
    private final SecurityUtils securityUtils;
    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;

    @GetMapping("/me")
    public RestResponse<CompanyResponse> getCompanyProfile(Authentication authentication) {
        Company company = companyService.getCompanyById(securityUtils.extractCompanyId(authentication));

        return RestResponse.<CompanyResponse>builder()
                .status(HttpStatus.OK)
                .data(companyMapper.toResponse(company))
                .build();
    }

    @GetMapping("/lookup")
    public RestResponse<List<HashMap<String,String>>> lookup(@RequestParam(required = false)String query) {
        List<HashMap<String,String>> result = companyService.lookup(query);
        return RestResponse.<List<HashMap<String, String>>>builder()
                .status(HttpStatus.OK)
                .data(result)
                .build();

    }

    @GetMapping("/{companyId}/jobs")
    public RestResponse<Page<JobResponse>> getJobsByCompanies(@PathVariable("companyId") String companyId,
                                                              @RequestParam(name = "page", defaultValue = "0") Integer page,
                                                              @RequestParam(name = "size", defaultValue = "10") Integer size) {

        if (companyId == null || companyId.isEmpty()) {
            throw new BadRequestException("Company ID invalid");
        }

        Pageable pageable = null;
        if (size == null) {
            pageable = PageRequest.of(0, 5);
        } else {
            pageable = PageRequest.of(page, size);
        }

        Page<Job> jobs = jobService.getJobsByCompany(companyId, pageable);

        return RestResponse.<Page<JobResponse>>builder()
                .status(HttpStatus.OK)
                .data(mapToJobResponsePage(jobs, pageable))
                .build();
    }

    // Helper method to map to response page
    private Page<JobResponse> mapToJobResponsePage(Page<Job> jobPage, Pageable pageable) {
        List<JobResponse> jobResponses = jobPage.stream()
                .map(job -> jobMapper.toResponse(job))
                .toList();
        return new PageImpl<>(jobResponses, pageable, jobPage.getTotalElements());
    }

    // Helper method to map to response page
    private List<JobResponse> mapToJobResponseList(List<Job> jobs) {
        List<JobResponse> jobResponses = jobs.stream()
                .map(job -> jobMapper.toResponse(job))
                .toList();
        return jobResponses;
    }
}
