package com.hcmute.careergraph.listener;

import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.event.CandidateUpdatedEvent;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.repositories.CandidateESRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hcmute.careergraph.services.EmbedService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Event listener for Candidate updates.
 * Handles synchronization between PostgreSQL and Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CandidateElasticsearchEventListener {

  private final CandidateRepository candidateRepository;
  private final CandidateESRepository candidateESRepository;
  private final EmbedService embedService;

  /**
   * Listen to CandidateUpdatedEvent and sync to Elasticsearch
   * Runs after transaction commits to ensure data consistency
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handleCandidateUpdated(CandidateUpdatedEvent event) {
    log.info("📥 Received CandidateUpdatedEvent: candidateId={}, type={}",
        event.candidateId(), event.updateType());

    try {
      Optional<Candidate> candidateOpt = candidateRepository.findById(event.candidateId());

      if (candidateOpt.isEmpty()) {
        log.warn("Candidate not found for id: {}", event.candidateId());
        return;
      }

      Candidate candidate = candidateOpt.get();

      // Check if candidate should be in Elasticsearch (only if open to work for some
      // events)
      boolean shouldIndex = shouldIndexCandidate(candidate, event.updateType());

      if (shouldIndex) {
        syncCandidateToElasticsearch(candidate);
        log.info("✅ Synced candidate {} to Elasticsearch", candidate.getId());
      } else {
        // Remove from Elasticsearch if exists and no longer should be indexed
        removeFromElasticsearchIfExists(candidate.getId());
        log.info("🗑️ Removed candidate {} from Elasticsearch (isOpenToWork=false)", candidate.getId());
      }

    } catch (Exception e) {
      log.error("❌ Error handling CandidateUpdatedEvent for candidateId={}: {}",
          event.candidateId(), e.getMessage(), e);
    }
  }

  /**
   * Determine if candidate should be indexed based on update type
   */
  private boolean shouldIndexCandidate(Candidate candidate, CandidateUpdatedEvent.CandidateUpdateType updateType) {
    // For job search status change, respect the isOpenToWork flag
    if (updateType == CandidateUpdatedEvent.CandidateUpdateType.JOB_SEARCH_STATUS_CHANGED) {
      return Boolean.TRUE.equals(candidate.getIsOpenToWork());
    }

    // For job criteria updates, always sync (candidate is actively updating their
    // profile)
    // They should be indexed regardless of isOpenToWork status
    return true;
  }

  /**
   * Sync a single candidate to Elasticsearch
   */
  private void syncCandidateToElasticsearch(Candidate candidate) {
    // Build search text for embedding
    String searchText = buildSearchText(candidate);

    // Generate embedding
    float[] embedding = embedService.embed(searchText);

    // Convert to CandidateES and save
    CandidateES candidateES = convertToCandidateES(candidate, embedding);
    candidateESRepository.save(candidateES);
  }

  /**
   * Remove candidate from Elasticsearch if exists
   */
  private void removeFromElasticsearchIfExists(String candidateId) {
    if (candidateESRepository.existsById(candidateId)) {
      candidateESRepository.deleteById(candidateId);
    }
  }

  /**
   * Build search text for embedding generation
   */
  private String buildSearchText(Candidate candidate) {
    StringBuilder sb = new StringBuilder();

    if (candidate.getDesiredPosition() != null) {
      sb.append(candidate.getDesiredPosition()).append(" ");
    }
    if (candidate.getCurrentJobTitle() != null) {
      sb.append(candidate.getCurrentJobTitle()).append(" ");
    }
    if (candidate.getSkills() != null && !candidate.getSkills().isEmpty()) {
      String skillNames = candidate.getSkills().stream()
          .filter(cs -> cs.getSkill() != null && cs.getSkill().getName() != null)
          .map(cs -> cs.getSkill().getName())
          .collect(Collectors.joining(" "));
      sb.append(skillNames).append(" ");
    }
    if (candidate.getSummary() != null) {
      sb.append(candidate.getSummary());
    }

    String result = sb.toString().trim();
    return result.isEmpty() ? "candidate profile" : result;
  }

  /**
   * Convert Candidate entity to CandidateES document
   */
  private CandidateES convertToCandidateES(Candidate candidate, float[] embedding) {
    // Extract skill names
    List<String> skillNames = new ArrayList<>();
    if (candidate.getSkills() != null) {
      skillNames = candidate.getSkills().stream()
          .filter(cs -> cs.getSkill() != null && cs.getSkill().getName() != null)
          .map(cs -> cs.getSkill().getName())
          .collect(Collectors.toList());
    }


    // Get email from account
    String email = null;
    if (candidate.getAccount() != null) {
      email = candidate.getAccount().getEmail();
    }

    return CandidateES.builder()
        .id(candidate.getId())
        .firstName(candidate.getFirstName())
        .lastName(candidate.getLastName())
        .email(email)
        .gender(candidate.getGender())
        .yearsOfExperience(candidate.getYearsOfExperience())
        .desiredPosition(candidate.getDesiredPosition())
        .currentJobTitle(candidate.getCurrentJobTitle())
        .summary(candidate.getSummary())
        .isOpenToWork(candidate.getIsOpenToWork() != null ? candidate.getIsOpenToWork() : false)
        .educationLevel(candidate.getEducationLevel())
        .experienceLevel(determineExperienceLevel(candidate.getYearsOfExperience()))
        .industries(candidate.getIndustries())
        .locations(candidate.getLocations())
        .workTypes(candidate.getWorkTypes())
        .salaryExpectationMin(candidate.getSalaryExpectationMin())
        .salaryExpectationMax(candidate.getSalaryExpectationMax())
        .skills(skillNames)
        .createdAt(candidate.getCreatedDate() != null ? candidate.getCreatedDate().toLocalDate() : LocalDate.now())
        .lastActive(LocalDate.now())
        .embedding(embedding)
        .build();
  }

  /**
   * Determine experience level based on years
   */
  private String determineExperienceLevel(Integer yearsOfExperience) {
    if (yearsOfExperience == null || yearsOfExperience == 0) {
      return "FRESHER";
    } else if (yearsOfExperience <= 2) {
      return "JUNIOR";
    } else if (yearsOfExperience <= 5) {
      return "MIDDLE";
    } else if (yearsOfExperience <= 10) {
      return "SENIOR";
    } else {
      return "EXPERT";
    }
  }
}
