package com.hcmute.careergraph.persistence.dtos.request;

import lombok.Data;

public class CandidateRequest {
    @Data
    public static class UpdateInformation {
        private String name;
        private String phone;
        private String province;
        private String district;
        private String dateOfBirth;
        private String gender;
        private Boolean isMarried;
    }

}
