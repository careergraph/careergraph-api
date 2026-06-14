package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record JobSettingsUpdateRequest(
        @NotNull(message = "aiScreeningEnabled is required")
        Boolean aiScreeningEnabled) {
}
