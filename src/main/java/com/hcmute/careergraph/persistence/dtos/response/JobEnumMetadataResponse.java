package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.List;

@Builder
public record JobEnumMetadataResponse(
        List<EnumOption> experienceLevels,
        List<EnumOption> employmentTypes,
        List<EnumOption> educationTypes,
        List<EnumOption> jobCategories
) {
    @Builder
    public record EnumOption(
            String code,
            String name
    ) {
    }
}
