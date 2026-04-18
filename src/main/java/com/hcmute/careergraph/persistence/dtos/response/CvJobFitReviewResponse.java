package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JSON trả về từ FastAPI /api/v1/review-cv (sau khi làm sạch markdown nếu có).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CvJobFitReviewResponse {

    private Integer matchScore;
    private String summary;
    private List<String> strengths;
    private List<String> gaps;
}
