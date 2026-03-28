package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.InterviewParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewParticipantRepository extends JpaRepository<InterviewParticipant, String> {

    List<InterviewParticipant> findByInterviewId(String interviewId);

    Optional<InterviewParticipant> findByInterviewIdAndAccountId(String interviewId, String accountId);

    boolean existsByInterviewIdAndAccountId(String interviewId, String accountId);
}
