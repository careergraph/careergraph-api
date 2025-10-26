package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.work.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String requirements;

    private String benefits;

    private String salaryRange;

    private String experienceLevel;

    private String workArrangement;

    private String postedDate;

    private String expiryDate;

    private Integer numberOfPositions;

    private String workLocation;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    private Boolean isUrgent;

    @NotBlank(message = "Company ID is required")
    private String companyId;
}
