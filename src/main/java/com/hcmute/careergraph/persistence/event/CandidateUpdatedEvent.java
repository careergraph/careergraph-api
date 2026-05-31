package com.hcmute.careergraph.persistence.event;

/**
 * Event fired when candidate job criteria or job search status is updated.
 * Triggers Elasticsearch synchronization.
 */
public record CandidateUpdatedEvent(
    String candidateId,
    CandidateUpdateType updateType) {
  public enum CandidateUpdateType {
    JOB_CRITERIA_UPDATED, // updateJobFindCriteriaInfo was called
    JOB_SEARCH_STATUS_CHANGED, // setJobSearchStatus was called
    RESUME_UPDATED,
    RESUME_DELETED,
    RESUME_VISIBILITY_CHANGED,
    RESUME_EXTRACTION_FAILED,
    PROFILE_UPDATED,
    SKILLS_UPDATED
  }
}
