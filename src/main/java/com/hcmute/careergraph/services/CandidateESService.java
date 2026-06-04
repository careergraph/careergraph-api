package com.hcmute.careergraph.services;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.dtos.request.CandidateFilterRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateSuggestionResponse;
import com.hcmute.careergraph.persistence.models.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for Candidate Elasticsearch operations
 * Supports hybrid search (fuzzy text + embedding) for candidate matching
 */
public interface CandidateESService {

  /**
   * Hybrid search candidates by keyword
   * Combines BM25 text search + KNN embedding search
   * Only returns candidates with isOpenToWork = true
   *
   * @param keyword  Search keyword (matches desiredPosition, currentJobTitle,
   *                 skills)
   * @param filter   Additional filters (education, experience, location, etc.)
   * @param pageable Pagination
   * @return SearchResponse with matched candidates
   */
  SearchResponse<CandidateES> hybridSearchCandidates(
      String keyword,
      CandidateFilterRequest filter,
      Pageable pageable);

  /**
   * Search candidates matching company's job titles
   * Used when no keyword is provided - matches job titles from company's active
   * jobs
   * with candidates' desiredPosition
   *
   * @param companyId Company ID to get active job titles
   * @param filter    Additional filters
   * @param pageable  Pagination
   * @return SearchResponse with matched candidates
   */
  SearchResponse<CandidateES> searchCandidatesForCompany(
      String companyId,
      CandidateFilterRequest filter,
      Pageable pageable);

  /**
   * Convert CandidateES to CandidateSuggestionResponse
   *
   * @param candidateES Elasticsearch document
   * @param score       Search relevance score
   * @return DTO response
   */
  CandidateSuggestionResponse toSuggestionResponse(CandidateES candidateES, Float score);

  /**
   * Index/update a candidate in Elasticsearch
   *
   * @param candidate JPA Candidate entity
   * @return Indexed CandidateES document
   */
  CandidateES indexCandidate(Candidate candidate);

  /**
   * Delete candidate from Elasticsearch index
   *
   * @param candidateId Candidate ID to delete
   */
  void deleteCandidate(String candidateId);

  /**
   * Sync all candidates from database to Elasticsearch
   * Used for initial indexing or reindexing
   *
   * @return Number of candidates indexed
   */
  int syncAllCandidates();

  /**
   * Sync specific candidate by ID to Elasticsearch
   * V2.1: Used by FileEventListener when CV changes
   *
   * @param candidateId Candidate ID to sync
   */
  void syncCandidate(String candidateId);
}
