package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.documents.CandidateES;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface CandidateESRepository extends ElasticsearchRepository<CandidateES, String> {

  /**
   * Search candidates by desired position with fuzzy matching
   */
  @Query("""
      {
          "bool": {
              "must": [
                  {
                      "multi_match": {
                          "query": "?0",
                          "fields": ["desiredPosition^10", "currentJobTitle^5", "skills^3", "summary^1"],
                          "fuzziness": "AUTO"
                      }
                  }
              ],
              "filter": [
                  { "term": { "isOpenToWork": true } }
              ]
          }
      }
      """)
  Page<CandidateES> searchByDesiredPosition(String keyword, Pageable pageable);

  /**
   * Find all candidates who are open to work
   */
  List<CandidateES> findByIsOpenToWorkTrue();

  /**
   * Find candidates by location
   */
  List<CandidateES> findByLocationsContainingAndIsOpenToWorkTrue(String location);
}
