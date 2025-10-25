package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CompanyRequest(
        @NotBlank(message = "Tagname is required")
        String tagname,
        String avatar,
        String cover,
        String size,
        String website,
        String ceoName,
        Integer noOfMembers,
        Integer yearFounded
) {
}
