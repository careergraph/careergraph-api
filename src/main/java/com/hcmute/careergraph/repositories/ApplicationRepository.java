package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    
    Page<Application> findByCandidateId(String candidateId, Pageable pageable);
    
    Page<Application> findByJobId(String jobId, Pageable pageable);
}
