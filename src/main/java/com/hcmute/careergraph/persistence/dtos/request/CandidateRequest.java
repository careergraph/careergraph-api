package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

// CandidateRequest.java
public final class CandidateRequest {

    private CandidateRequest() {}

    @Builder
    public record UpdateInformationRequest(
            // 1. Thông tin cơ bản của ứng viên
            String firstName,
            String lastName,
            String gender,        // "MALE" | "FEMALE" | ...
            String dateOfBirth,   // "YYYY-MM-DD"
            Boolean isMarried,    // true nếu đã lập gia đình

            // 2. Liên hệ (contact): ví dụ phone
            ContactDTO contact,

            // 3. Địa chỉ hiện tại
            AddressDTO address
    ) {
    }

    @Builder
    public record ContactDTO(
            String type,       // e.g. "PHONE", "EMAIL"
            String value,      // "0912345678"
            Boolean isPrimary  // true nếu số chính
    ) {
    }

    @Builder
    public record AddressDTO(
            String country,    // "VN"
            String province,   // "TP. Hồ Chí Minh"
            String district,   // "Quận 1"
            String ward,       // "Phường Bến Nghé"
            Boolean isPrimary  // true nếu địa chỉ chính
    ) {
    }


    @Builder
    public record UpdateJobCriteriaRequest(
            String desiredPosition,
            List<String> industries,
            List<String> locations,
            Integer  salaryExpectationMin,
            Integer  salaryExpectationMax,
            List<String> workTypes
    ) {
    }

    @Builder
    public record UpdateGeneralInfo(
            Integer yearsOfExperience,
            String educationLevel,
            String currentPosition
    ) {
    }

    @Builder
    public record CandidateExperienceRequest(
            String startDate,
            String endDate,
            Integer salary,
            String jobTitle,
            Boolean isCurrent,
            String description,
            String candidateId,
            String companyId,
            String companyName
    ) {
    }


    @Builder
    public record CandidateEducationRequest(
            String startDate,
            String endDate,
            String degreeTitle,
            Boolean isCurrent,
            String description,
            String universityId,
            String officialName,
            String major
    ){}

//    @Builder
//    public record ReplaceSkillsRequest (
//            List<String> skills
//    ){}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplaceSkillsRequest {
        @NotNull
        private List<String> skills;
    }

}