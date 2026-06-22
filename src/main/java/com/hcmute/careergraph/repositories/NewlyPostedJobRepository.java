package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.NewlyPostedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Collection;

public interface NewlyPostedJobRepository extends JpaRepository<NewlyPostedJob, String> {

  boolean existsByJobId(String jobId);

  @Query("SELECT n.jobId FROM NewlyPostedJob n")
  List<String> findAllJobIds();

  void deleteByJobId(String jobId);

  long deleteByJobIdIn(Collection<String> jobIds);
}
