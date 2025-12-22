package com.hcmute.careergraph.services;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobESService {
    Page<JobES> searchPosts(String key, Pageable pageable);

    SearchResponse<JobES> searchJobsByNavtive(String key, Pageable pageable);
    SearchResponse<JobES> searchJobsByNavtiveAndFuzzy(String key, Pageable pageable);
    SearchResponse<JobES> knnSearch(float[] queryVector, int k) ;
    SearchResponse<JobES> knnSearch(
            float[] vector,
            JobFilterRequest filter,
            String partyId,
            Pageable pageable,
            PartyType type
    );
}
