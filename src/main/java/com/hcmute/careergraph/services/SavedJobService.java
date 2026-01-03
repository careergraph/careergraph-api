package com.hcmute.careergraph.services;

public interface SavedJobService {
    void saveJob(String candidateId, String jobId);
    void unsaveJob(String candidateId, String jobId);
    boolean existsByCandidateIdAndJobId(String candidateId, String jobId);
}
