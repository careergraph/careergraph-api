package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.List;
import java.util.Set;

// CandidateResponse.java
public final class CandidateClientResponse {

    private CandidateClientResponse() {}

    @Builder
    public record CandidateProfileResponse(
            String candidateId,

            // Basic info
            String firstName,
            String lastName,
            String email,        // lấy từ Account hoặc từ contact type=EMAIL
            String gender,       // "MALE" | "FEMALE"
            String dateOfBirth,  // "YYYY-MM-DD"
            Boolean isMarried,

            // Contact chính (ví dụ phone)
            Set<ContactResponse> contacts,

            // Địa chỉ chính
            Set<AddressResponse> addresses

    ) {}

    @Builder
    public record ContactDTO(
            String type,        // "PHONE"
            String value,       // "0912345678"
            Boolean verified,   // từ entity Contact.verified
            Boolean isPrimary
    ) {}

    @Builder
    public record AddressDTO(
            String country,
            String province,
            String district,
            String ward,
            Boolean isPrimary
    ) {}

    @Builder
    public record CandidateJobCriteriaResponse(
            // 1. Thông tin cơ bản của ứng viên
            String desiredPosition,
            List<String> industries,
            List<String> locations,        // "MALE" | "FEMALE" | ...
            Integer  salaryExpectationMin,
            Integer  salaryExpectationMax,

            List<String> workTypes
    ) {
    }

    @Builder
    public record GeneralInfoResponse(
            Integer yearsOfExperience,
            String educationLevel,
            String currentPosition
    ) {
    }
}
