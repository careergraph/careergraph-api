package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EducationRequest {

    @NotBlank(message = "Tagname is required")
    private String tagname;

    private String avatar;

    private String cover;

    private String startDate;

    private String endDate;

    private String description;

    private Boolean isCurrentlyStudying;
}
