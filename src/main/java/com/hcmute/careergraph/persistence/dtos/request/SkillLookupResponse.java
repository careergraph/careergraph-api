package com.hcmute.careergraph.persistence.dtos.request;

import lombok.Builder;

@Builder
public record SkillLookupResponse(String id, String name, String category) {
    public record AddressRequest() {
    }

    public record ContactRequest() {
    }
}
