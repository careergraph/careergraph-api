package com.hcmute.careergraph.persistence.dtos.record;

import lombok.Builder;

@Builder
public record SkillLookupResponse(String id, String name, String category) {
    public static record AddressRequest() {
    }

    public static record ContactRequest() {
    }
}
