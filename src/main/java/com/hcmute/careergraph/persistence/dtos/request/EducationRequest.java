package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record EducationRequest(
        @NotBlank(message = "Tagname is required")
        String tagname,
        String avatar,
        String cover,
        String startDate,
        String endDate,
        String description,
        Boolean isCurrentlyStudying
) {
}
