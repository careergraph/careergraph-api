package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

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
            ContactDTO primaryContact,

            // Địa chỉ chính
            AddressDTO primaryAddress

            // (Optional) avatarUrl nếu bạn muốn embed luôn
            // String avatarUrl
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
}
