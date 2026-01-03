package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SavedJobRepository extends JpaRepository<SavedJob,String> {

    boolean existsByCandidateIdAndJobId(String candidateId, String jobId);
    SavedJob findByCandidateIdAndJobId(String candidateId, String jobId);

    @Query("select s.job from SavedJob s where s.candidate.id = :candidateId")
    List<Job> findAllByCandidateId(String candidateId);
}
