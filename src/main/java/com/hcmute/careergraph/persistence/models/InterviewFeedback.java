package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.interview.FeedbackRecommendation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "interview_feedback",
        uniqueConstraints = @UniqueConstraint(columnNames = {"interview_id", "reviewer_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"interview", "reviewer"})
@EqualsAndHashCode(callSuper = true, exclude = {"interview", "reviewer"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Account reviewer;

    @Column(name = "overall_rating", nullable = false)
    private Integer overallRating;

    @Column(name = "technical_score")
    private Integer technicalScore;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "culture_fit_score")
    private Integer cultureFitScore;

    @Column(name = "problem_solving_score")
    private Integer problemSolvingScore;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 20)
    private FeedbackRecommendation recommendation;
}
