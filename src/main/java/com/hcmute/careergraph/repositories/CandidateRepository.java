package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Candidate;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, String> {
    List<Candidate> findAllByIsOpenToNotifyNewJob(Boolean isOpenToNotifyNewJob);
    
    /**
     * V2.1: Fetch candidate với eager loading cho collections
     * Tránh LazyInitializationException khi sync ES trong async context
     */
    @Query("""
        SELECT DISTINCT c FROM Candidate c
        LEFT JOIN FETCH c.account
        LEFT JOIN FETCH c.contacts
        LEFT JOIN FETCH c.skills cs
        LEFT JOIN FETCH cs.skill
        WHERE c.id = :candidateId
    """)
    java.util.Optional<Candidate> findByIdWithCollections(@Param("candidateId") String candidateId);

    @Query("SELECT c.id FROM Candidate c")
    List<String> findAllIds();
}
