package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    
    Page<Application> findByCandidateId(String candidateId, Pageable pageable);
    
    Page<Application> findByJobId(String jobId, Pageable pageable);

    @Query("""
        SELECT a
        FROM Application a
        WHERE a.job.id = :jobId
            AND a.job.company.id = :companyId
    """)
    List<Application> findByCompanyIdAndJobId(@Param("companyId") String companyId, @Param("jobId") String jobId);
}
