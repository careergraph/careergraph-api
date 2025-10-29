package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record CompanyResponse(
        String companyId,
        String tagname,
        String name,
        String avatar,
        String cover,
        int noOfFollowers,
        int noOfFollowing,
        int noOfConnections,

        // Company-specific fields
        String size,
        String website,
        String ceoName,
        Integer noOfMembers,
        Integer yearFounded,

        // Shared contact & address
        Set<ContactResponse> contacts,
        Set<AddressResponse> addresses,

        Set<ConnectionResponse> companyConnections,

        Set<JobResponse> jobs
) {
    public CompanyResponse {
        contacts = contacts != null ? contacts : Set.of();
        addresses = addresses != null ? addresses : Set.of();
        companyConnections = companyConnections != null ? companyConnections : Set.of();
        jobs = jobs != null ? jobs : Set.of();
    }
}
