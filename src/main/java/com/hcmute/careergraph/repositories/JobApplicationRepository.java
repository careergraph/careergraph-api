package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, String> {
}
