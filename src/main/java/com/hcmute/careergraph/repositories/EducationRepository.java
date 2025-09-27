package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EducationRepository extends JpaRepository<Education, String> {
}
