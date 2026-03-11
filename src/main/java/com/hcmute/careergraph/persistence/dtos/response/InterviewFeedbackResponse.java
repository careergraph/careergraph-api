package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.interview.FeedbackRecommendation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewFeedbackResponse {

    private String id;
    private String interviewId;
    private String reviewerId;
    private String reviewerName;
    private Integer overallRating;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer cultureFitScore;
    private Integer problemSolvingScore;
    private String strengths;
    private String weaknesses;
    private String notes;
    private FeedbackRecommendation recommendation;
    private LocalDateTime createdDate;
}
