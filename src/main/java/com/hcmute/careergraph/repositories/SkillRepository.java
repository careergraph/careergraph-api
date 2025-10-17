package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {
    
    Page<Skill> findByCategory(String category, Pageable pageable);

    @Query("""
    SELECT s FROM Skill s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    List<Skill> lookupSkill(@Param("query") String query);
}
