package com.hcmute.careergraph.persistence.dtos.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CvReviewRequest {
    @JsonProperty("cvData")
    private CvData cvData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvData {
        private PersonalInfo personal;
        private ContactInfo contact;
        private List<Experience> experience;
        private List<Education> education;
        private List<Skill> skills;
        private List<Language> languages;
        private List<Award> awards;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PersonalInfo {
            private String fullName;
            private String headline;
            private String summary;
            private String location;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ContactInfo {
            private String email;
            private String phone;
            private String website;
            private String linkedin;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Experience {
            private String id;
            private String role;
            private String company;
            private String location;
            private String startDate;
            private String endDate;
            private List<String> bulletPoints;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Education {
            private String id;
            private String school;
            private String degree;
            private String startDate;
            private String endDate;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Skill {
            private String id;
            private String name;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Language {
            private String id;
            private String language;
            private String name;
            private String proficiency;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Award {
            private String id;
            private String title;
            private String issuer;
            private String year;
        }
    }
}
