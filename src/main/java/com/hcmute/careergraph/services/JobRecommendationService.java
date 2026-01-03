package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Job;

import java.util.List;

public interface JobRecommendationService {

    void recommendJobsForCandidate(
            Candidate candidate,
            int limit
    );
}
