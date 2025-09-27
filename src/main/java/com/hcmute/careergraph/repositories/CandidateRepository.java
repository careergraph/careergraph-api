package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, String> {
}
