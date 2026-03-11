package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.interview.ProposalStatus;
import com.hcmute.careergraph.persistence.models.InterviewTimeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewTimeProposalRepository extends JpaRepository<InterviewTimeProposal, String> {

    List<InterviewTimeProposal> findByInterviewIdOrderByCreatedDateAsc(String interviewId);

    List<InterviewTimeProposal> findByInterviewIdAndProposalStatus(String interviewId, ProposalStatus status);
}
