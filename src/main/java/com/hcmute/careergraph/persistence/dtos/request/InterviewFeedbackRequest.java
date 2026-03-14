package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedbackRequest {

    @NotNull(message = "Overall rating is required")
    @Min(1)
    @Max(5)
    private Integer overallRating;

    @Min(1) @Max(10)
    private Integer technicalScore;

    @Min(1) @Max(10)
    private Integer communicationScore;

    @Min(1) @Max(10)
    private Integer cultureFitScore;

    @Min(1) @Max(10)
    private Integer problemSolvingScore;

    private String strengths;

    private String weaknesses;

    @NotBlank(message = "Recommendation is required")
    private String recommendation;

    private String notes;
}
