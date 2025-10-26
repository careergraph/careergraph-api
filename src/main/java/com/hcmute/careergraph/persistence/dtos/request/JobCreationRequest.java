package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.work.EducationType;
import com.hcmute.careergraph.enums.work.EmploymentType;
import com.hcmute.careergraph.enums.work.ExperienceLevel;
import com.hcmute.careergraph.enums.work.JobCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record JobCreationRequest(
        // Basic Information
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        String department, // Optional

        // Job Details - Arrays
        List<String> responsibilities,
        List<String> qualifications,
        List<String> minimumQualifications,
        List<String> benefits,

        // Experience Requirements
        @NotNull(message = "Minimum experience is required")
        Integer minExperience,

        @NotNull(message = "Maximum experience is required")
        Integer maxExperience,

        @NotNull(message = "Experience level is required")
        ExperienceLevel experienceLevel,

        // Job Type & Category
        @NotNull(message = "Employment type is required")
        EmploymentType employmentType,

        @NotNull(message = "Job category is required")
        JobCategory jobCategory,

        EducationType education, // Optional

        // Location
        Boolean remoteJob, // Default false nếu null

        @NotBlank(message = "State/Province is required")
        String state, // Mã tỉnh/thành phố từ API location

        @NotBlank(message = "City/District is required")
        String city, // Mã quận/huyện từ API location

        String district, // Mã phường/xã từ API location (optional)
        String address, // Địa chỉ cụ thể (optional)

        // Skills
        List<String> skillIds, // List ID của skills cần thiết

        // Compensation & Contact
        String salaryRange,

        @Email(message = "Invalid email format")
        String contactEmail,

        String contactPhone,

        // Posting Information
        String promotionType, // "free" or "paid"
        Integer numberOfPositions, // Số lượng vị trí cần tuyển
        String expiryDate // Ngày hết hạn đăng tin (format: yyyy-MM-dd hoặc ISO string)
) {
    /**
     * Constructor với default values
     */
    public JobCreationRequest {
        // Set default values if null
        if (remoteJob == null) {
            remoteJob = false;
        }
        if (promotionType == null || promotionType.isBlank()) {
            promotionType = "free";
        }
        if (numberOfPositions == null) {
            numberOfPositions = 1;
        }

        // Validate experience range
        if (minExperience != null && maxExperience != null && minExperience > maxExperience) {
            throw new IllegalArgumentException("Minimum experience cannot be greater than maximum experience");
        }
    }
}
