package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    
    Page<Job> findByCompanyId(String companyId, Pageable pageable);
}
