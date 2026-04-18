package com.hcmute.careergraph.persistence.dtos.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body gửi tới FastAPI POST /api/v1/review-cv (camelCase, Jackson).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CvJobFitReviewRequest {

    private String jobTitle;
    private String companyName;
    private String jobDescription;
    private String jobQualificationsText;
    private String candidateProfileText;
    private String coverLetter;
}
