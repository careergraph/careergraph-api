package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CvReviewResponse {
    private Integer overallScore;
    private String summary;
    private List<String> strengths;
    private List<Improvement> improvements;
    private JobFit jobFit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Improvement {
        private String section;
        private String priority;
        private String suggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobFit {
        private Integer matchScore;
        private List<String> matchedRequirements;
        private List<String> missingRequirements;
    }
}
