package com.hcmute.careergraph.persistence.dtos;

import com.hcmute.careergraph.enums.EmploymentType;
import com.hcmute.careergraph.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {

    private String jobId;

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

    private EmploymentType employmentType;

    private Status status;

    private Boolean isUrgent;

    private CompanyDto company;

    private Set<JobSkillDto> requiredSkills;

    private Set<ApplicationDto> applications;
}
