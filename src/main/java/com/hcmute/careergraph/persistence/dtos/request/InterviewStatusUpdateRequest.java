package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String reason;
}
