package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, String> {
}
