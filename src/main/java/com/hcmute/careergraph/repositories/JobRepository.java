package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    
    List<Job> findByCompanyId(String companyId);
}
