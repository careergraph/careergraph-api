package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

@Builder
public record AddressResponse(
        String addressId,
        String name,
        String country,
        String province,
        String district,
        String ward,
        Boolean isPrimary,
        String partyId
) {
}
