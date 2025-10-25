package com.hcmute.careergraph.persistence.dtos.request;

import lombok.Builder;

public final class CandidateRequest {

    private CandidateRequest() {
    }

    @Builder
    public record UpdateInformation(
            String name,
            String phone,
            String province,
            String district,
            String dateOfBirth,
            String gender,
            Boolean isMarried
    ) {
    }
}
