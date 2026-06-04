package com.hcmute.careergraph.controllers;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.dtos.request.CandidateFilterRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateSuggestionResponse;
import com.hcmute.careergraph.services.CandidateESService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Candidate Suggestion/Search functionality
 * Used by HR/Company to find suitable candidates
 */
@RestController
@Slf4j
@RequestMapping("candidates/suggestion")
public class CandidateSuggestionController {

  private final CandidateESService candidateESService;
  private final SecurityUtils securityUtils;

  public CandidateSuggestionController(CandidateESService candidateESService, SecurityUtils securityUtils) {
    this.candidateESService = candidateESService;
    this.securityUtils = securityUtils;
  }

  /**
   * POST /api/v1/candidates/suggestion/search
   * Search candidates with hybrid search (fuzzy text + embedding)
   * 
   * If keyword is empty/null: matches company's job titles with candidates'
   * desiredPosition
   * If keyword is provided: matches keyword with candidates' desiredPosition
   * Only returns candidates with isOpenToWork = true
   *
   * @param keyword        Search keyword (optional)
   * @param filter         Additional filters
   * @param page           Page number (default: 0)
   * @param size           Page size (default: 10)
   * @param authentication Authentication to get company ID
   * @return Page of CandidateSuggestionResponse
   */
  @PostMapping("/search")
  public RestResponse<Page<CandidateSuggestionResponse>> searchCandidates(
      @RequestParam(required = false, defaultValue = "") String keyword,
      @RequestBody(required = false) CandidateFilterRequest filter,
      @RequestParam(name = "page", defaultValue = "0") Integer page,
      @RequestParam(name = "size", defaultValue = "10") Integer size,
      Authentication authentication) {
    log.info("POST /api/v1/candidates/suggestion/search - keyword: {}, page: {}, size: {}",
        keyword, page, size);

    String companyId = securityUtils.extractCompanyId(authentication);
    Pageable pageable = PageRequest.of(page, size);

    // Initialize filter if null
    if (filter == null) {
      filter = new CandidateFilterRequest();
    }

    SearchResponse<CandidateES> response;

    if (keyword == null || keyword.trim().isEmpty()) {
      // No keyword - match company's job titles with candidates
      log.debug("No keyword provided, searching based on company's job titles");
      response = candidateESService.searchCandidatesForCompany(companyId, filter, pageable);
    } else {
      // Keyword provided - hybrid search
      log.debug("Searching candidates with keyword: {}", keyword);
      response = candidateESService.hybridSearchCandidates(keyword.trim(), filter, pageable);
    }

    // Convert response to DTOs with normalized scores (V2)
    List<CandidateSuggestionResponse> candidates = new ArrayList<>();
    if (response != null && response.hits() != null && response.hits().hits() != null) {
      // Get maxScore for normalization
      float maxScore = 0f;
      for (Hit<CandidateES> hit : response.hits().hits()) {
        Double scoreValue = hit.score();
        if (scoreValue != null && scoreValue.floatValue() > maxScore) {
          maxScore = scoreValue.floatValue();
        }
      }
      
      // Convert hits with normalized scores
      for (Hit<CandidateES> hit : response.hits().hits()) {
        if (hit.source() != null) {
          Double scoreValue = hit.score();
          float rawScore = scoreValue != null ? scoreValue.floatValue() : 0f;
          // Normalize: (rawScore / maxScore) * 100
          float normalizedScore = maxScore > 0 ? (rawScore / maxScore) * 100f : 0f;
          normalizedScore = Math.min(normalizedScore, 100f);
          
          CandidateSuggestionResponse dto = candidateESService.toSuggestionResponse(
              hit.source(),
              normalizedScore);
          candidates.add(dto);
        }
      }
    }

    // Get total count
    long total = 0;
    if (response != null && response.hits() != null) {
      var totalHits = response.hits().total();
      if (totalHits != null) {
        total = totalHits.value();
      }
    }

    Page<CandidateSuggestionResponse> resultPage = new PageImpl<>(
        candidates,
        pageable,
        total);

    return RestResponse.<Page<CandidateSuggestionResponse>>builder()
        .status(HttpStatus.OK)
        .message("Search candidates successfully")
        .data(resultPage)
        .build();
  }

  /**
   * GET /api/v1/candidates/suggestion/{candidateId}
   * Get detailed candidate information for suggestion view
   *
   * @param candidateId Candidate ID
   * @return CandidateSuggestionResponse with full details
   */
  @GetMapping("/{candidateId}")
  public RestResponse<CandidateSuggestionResponse> getCandidateDetail(
      @PathVariable String candidateId) {
    log.info("GET /api/v1/candidates/suggestion/{}", candidateId);

    // This would typically fetch from database for full details
    // For now, returning from ES index
    // You may want to enhance this to fetch from JPA repository for complete data

    return RestResponse.<CandidateSuggestionResponse>builder()
        .status(HttpStatus.OK)
        .message("Get candidate detail successfully")
        .data(null) // TODO: Implement full candidate detail fetch
        .build();
  }

  /**
   * POST /api/v1/candidates/suggestion/sync
   * Sync all candidates to Elasticsearch
   * Admin only endpoint
   *
   * @return Number of candidates synced
   */
  @PostMapping("/sync")
  public RestResponse<Integer> syncCandidates() {
    log.info("POST /api/v1/candidates/suggestion/sync - Syncing all candidates to ES");

    int count = candidateESService.syncAllCandidates();

    return RestResponse.<Integer>builder()
        .status(HttpStatus.OK)
        .message("Synced " + count + " candidates to Elasticsearch")
        .data(count)
        .build();
  }
}
