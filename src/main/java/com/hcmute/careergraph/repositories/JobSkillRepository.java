package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSkillRepository extends JpaRepository<JobSkill, String> {
}
