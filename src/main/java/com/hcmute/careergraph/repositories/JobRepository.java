package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    
    List<Job> findByCompanyId(String companyId);

    Page<Job> findByCompanyId(String companyId, Pageable pageable);

    @Query("""
        select j from Job j where j.id = :jobId and j.company.id = :companyId
    """)
    Optional<Job> findByIdAndCompanyId(@Param("jobId") String jobId, @Param("companyId") String companyId);
}
