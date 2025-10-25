package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SkillRequest(
        @NotBlank(message = "Name is required")
        String name,
        String category,
        String description
) {
}
