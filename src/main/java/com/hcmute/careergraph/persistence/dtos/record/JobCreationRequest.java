package com.hcmute.careergraph.persistence.dtos.record;

import com.hcmute.careergraph.enums.work.EmploymentType;
import com.hcmute.careergraph.enums.work.ExperienceLevel;
import com.hcmute.careergraph.enums.work.JobCategory;
import lombok.Builder;

import java.util.List;

@Builder
public record JobCreationRequest(
        String title,

        List<String> responsibilities,
        List<String> qualifications,
        List<String>minimumQualifications,

        Integer minExperience,
        Integer maxExperience,
        ExperienceLevel experienceLevel,

        EmploymentType employmentType,
        JobCategory jobCategory,

        Boolean isRemoteJob,
        String city,
        String district,
        String address, // ward + specific address

        List<Long> skillId,

        String salaryRange,
        String contactEmail,
        String contactPhone,
        String promotePaid, // FREE or PAID
        Integer numberOfPosition,
        String experienceDate
) { }
