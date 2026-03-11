package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.InterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, String> {

    List<InterviewFeedback> findByInterviewId(String interviewId);

    Optional<InterviewFeedback> findByInterviewIdAndReviewerId(String interviewId, String reviewerId);

    boolean existsByInterviewIdAndReviewerId(String interviewId, String reviewerId);
}
