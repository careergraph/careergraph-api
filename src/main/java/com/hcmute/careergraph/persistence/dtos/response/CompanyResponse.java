package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
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

        // Account info
        String role,
        String email,

        // Company-specific fields
        String size,
        String website,
        String ceoName,
        String description,
        Integer noOfMembers,
        Integer yearFounded,
        Boolean offerBeforeTrial,
        Boolean enableOffboardedStage,
        CompanyVerificationStatus verificationStatus,
        CompanyOperationalStatus operationalStatus,
        String taxCode,
        String legalRepresentativeName,
        String verificationBusinessEmail,
        String verificationWebsite,
        String verificationAdminNote,
        String blockReason,

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
