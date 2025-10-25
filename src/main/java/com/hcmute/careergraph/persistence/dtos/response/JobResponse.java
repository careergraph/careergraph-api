package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.work.EmploymentType;
import com.hcmute.careergraph.enums.work.ExperienceLevel;
import com.hcmute.careergraph.enums.work.JobCategory;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.JobSkill;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {

    private String jobId;

    private String title;

    /**
     * Field JSON for converter (UI Job): responsibilities
     */
    private List<String> responsibilities;

    /**
     * Field JSON for converter (UI Job): qualifications
     */
    private List<String> qualifications;

    /**
     * Field JSON for converter (UI Job): qualifications
     */
    private List<String> benefits;

    private String description;

    /**
     * Fields JOB detail
     */
    private String salaryRange;

    private Integer minExperience;

    private Integer maxExperience;

    private ExperienceLevel experienceLevel;

    private EmploymentType employmentType;

    private JobCategory jobCategory;

    /**
     * Post information for JOB
     */
    private String postedDate;

    private String expiryDate;

    private Integer numberOfPositions;

    private String contactEmail;

    private String contactPhone;

    private String promotionType; // "free" or "paid"

    /**
     * Address for JOB
     */
    private String city;

    private String district;

    private String address;

    private boolean remoteJob;

    /**
     * Stats fields for JOB
     */
    private Integer views = 0;

    private Integer applicants = 0;

    private Integer saved = 0;

    private Integer liked = 0;

    private Integer shared = 0;

    private Status status;

    private CompanyDto company;

    private Set<JobSkillDto> requiredSkills;

    private Set<ApplicationDto> applications;
}
