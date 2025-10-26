package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.work.EducationType;
import com.hcmute.careergraph.enums.work.EmploymentType;
import com.hcmute.careergraph.enums.work.ExperienceLevel;
import com.hcmute.careergraph.enums.work.JobCategory;
import lombok.Builder;

import java.util.List;

@Builder
public record JobResponse(
        // Basic Info
        String id,
        String title,
        String description,
        String department,

        // Arrays
        List<String> responsibilities,
        List<String> qualifications,
        List<String> minimumQualifications,
        List<String> benefits,

        // Experience
        Integer minExperience,
        Integer maxExperience,
        ExperienceLevel experienceLevel,

        // Job Type
        EmploymentType employmentType,
        JobCategory jobCategory,
        EducationType education,

        // Location - Map với UI
        String state,      // UI: state (tỉnh)
        String city,       // UI: city (quận/huyện)
        String district,   // UI: district (phường/xã)
        String specific,   // UI: specific (địa chỉ cụ thể) - map từ address
        Boolean remoteJob,

        // Skills - Return full skill objects
        List<SkillLookupResponse> skills,

        // Compensation & Contact
        String salaryRange,
        String contactEmail,
        String contactPhone,

        // Posting Info
        String postedDate,
        String expiryDate,
        Integer numberOfPositions,
        String promotionType,
        Status status,

        // Stats
        Integer views,
        Integer applicants,
        Integer saved,
        Integer likes,
        Integer shares,

        // Timeline (optional - for detail view)
        List<JobTimelineEventResponse> timeline,

        // Mapping field 'type' cho UI (UI dùng 'type' thay vì 'employmentType')
        // Sẽ được set = employmentType trong mapper
        EmploymentType type,

        // UI dùng 'jobFunction' - Map từ jobCategory
        JobCategory jobFunction
) {
    /**
     * Nested DTO cho Skill lookup
     */
    @Builder
    public record SkillLookupResponse(
            String id,
            String name
    ) {
    }

    /**
     * Nested DTO cho Timeline events
     */
    @Builder
    public record JobTimelineEventResponse(
            String date,
            String action,
            String description,
            String user
    ) {
    }
}