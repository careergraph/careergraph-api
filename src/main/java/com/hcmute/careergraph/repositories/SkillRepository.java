package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, String> {
}
