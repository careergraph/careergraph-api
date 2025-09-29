package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {
    
    Page<Skill> findByCategory(String category, Pageable pageable);
}
