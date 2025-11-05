package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, String> {
    @Query("""
        SELECT e.id, e.officialName
        FROM Education AS e
        WHERE lower(e.officialName) LIKE (lower(concat('%', :query, '%') ))
        """)
    List<Object[]> lookup(@Param("query") String query);
}
