package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record CompanyResponse(
        String companyId,
        String tagname,
        String avatar,
        String cover,
        String size,
        String website,
        String ceoName,
        int noOfMembers,
        int yearFounded,
        Set<JobResponse> jobs
) {
    public CompanyResponse {
        jobs = jobs != null ? jobs : Set.of();
    }
}
