package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.CandidateEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateEducationRepository extends JpaRepository<CandidateEducation,String> {
}
