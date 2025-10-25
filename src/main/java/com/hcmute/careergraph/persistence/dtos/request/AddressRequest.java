package com.hcmute.careergraph.persistence.dtos.request;

import lombok.Builder;

public final class AddressRequest {

    private AddressRequest() {
    }

    @Builder
    public record AddressUpdate(
            String province,
            String district
    ) {
    }
}
