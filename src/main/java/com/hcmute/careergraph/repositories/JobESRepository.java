package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.documents.JobES;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobESRepository extends ElasticsearchRepository<JobES, String> {

    @Query("""
    {
    "query": {
        "multi_match": {
        "query": "?0",
        "fields": ["title", "description", "state"]
        }
    }
    }
    """)
    Page<JobES> search(String keyword, Pageable pageable);
}
