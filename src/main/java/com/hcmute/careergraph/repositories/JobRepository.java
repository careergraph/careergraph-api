package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, String> {
}
