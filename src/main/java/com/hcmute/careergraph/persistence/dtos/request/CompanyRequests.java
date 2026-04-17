package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

public final class CompanyRequests {

    private CompanyRequests() {
    }

    @Builder
    public record UpdateMyProfileRequest(
            String name,
            String ceoName,
            String website,
            String size,
            Integer noOfMembers,
            @Min(1800) @Max(2500) Integer yearFounded,
            ContactDTO contact,
            AddressDTO address,
            String avatar,
            String cover
    ) {
    }

    @Builder
    public record ContactDTO(
            String type,
            String value,
            Boolean isPrimary
    ) {
    }

    @Builder
    public record AddressDTO(
            String country,
            String province,
            String district,
            String ward,
            Boolean isPrimary
    ) {
    }
}
