package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {

    /**
     * Find skill by category
     * @param category JobCategory: category of job
     * @param pageable Page: page of skill
     * @return Page: skill of category
     */
    Page<Skill> findByCategory(String category, Pageable pageable);

    @Query("""
    SELECT s FROM Skill s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    List<Skill> lookupSkill(@Param("query") String query);

    Optional<Skill> findByName(String name);
}
