package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.repositories.CandidateESRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Initializes Elasticsearch with Candidate data on application startup.
 * Syncs all candidates from PostgreSQL to Elasticsearch with embeddings.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Order(2) // Run after JobES initializer
@Slf4j
public class CandidateElasticsearchDataInitializer implements CommandLineRunner {

  private final CandidateRepository candidateRepository;
  private final CandidateESRepository candidateESRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final FileRepository fileRepository;
  private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;
  private final EmbeddingModel embeddingModel;

  @Value("${APP_ES_SYNC_CANDIDATES_ENABLED:false}")
  private boolean syncCandidatesEnabled;

  private static final int EMBEDDING_BATCH_SIZE = 100;
  private static final int MAX_RETRIES = 5;
  private static final long DELAY_SECONDS = 10;

  @Override
  public void run(String... args) throws Exception {
    if (!syncCandidatesEnabled) {
      log.info("Skip Candidate Elasticsearch synchronization because APP_ES_SYNC_CANDIDATES_ENABLED=false");
      return;
    }
    synchronizeCandidatesWithRetry();
  }

  private void synchronizeCandidatesWithRetry() {
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        // Check if index exists
        if (!elasticsearchOperations.indexOps(CandidateES.class).exists()) {
          log.warn("CandidateES index not found. Waiting for Spring Data to create or retrying...");
          throw new IllegalStateException("CandidateES index does not exist yet.");
        }

        log.info("Attempt {}: Starting Candidate data synchronization to Elasticsearch...", attempt);

        // Get all candidates from PostgreSQL
        List<Candidate> allCandidates = candidateRepository.findAll();

        if (allCandidates.isEmpty()) {
          log.info("No candidates found in database. Skipping synchronization.");
          return;
        }

        List<String> resumeTexts = allCandidates.stream()
          .map(candidate -> resolveResumeText(candidate.getId()))
          .toList();

        // Build search texts for embedding
        List<String> searchTexts = IntStream.range(0, allCandidates.size())
          .mapToObj(i -> buildSearchTextFromCandidate(allCandidates.get(i), resumeTexts.get(i)))
            .toList();

        // Generate embeddings in batches
        List<float[]> embeddings = generateEmbeddingsInBatches(searchTexts);

        // Clear existing data
        clearIndexData();

        // Convert to CandidateES documents and save
        List<CandidateES> candidatesToSave = IntStream.range(0, allCandidates.size())
          .mapToObj(i -> convertToCandidateES(allCandidates.get(i), resumeTexts.get(i), embeddings.get(i)))
            .toList();

        candidateESRepository.saveAll(candidatesToSave);

        log.info("✅ Candidate data synchronization complete. Total candidates: {}", allCandidates.size());
        return;

      } catch (Exception e) {
        log.error("Attempt {} failed. Reason: {}", attempt, e.getMessage());

        if (attempt < MAX_RETRIES) {
          log.info("Retrying in {} seconds...", DELAY_SECONDS);
          try {
            TimeUnit.SECONDS.sleep(DELAY_SECONDS);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Synchronization thread was interrupted during delay.");
            return;
          }
        } else {
          log.error("Failed to synchronize Candidate data to Elasticsearch after {} attempts.", MAX_RETRIES);
        }
      }
    }
  }

  /**
   * Build search text from Candidate entity for embedding generation
   */
  private String buildSearchTextFromCandidate(Candidate candidate, String resumeText) {
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
    if (StringUtils.hasText(resumeText)) {
      String snippet = resumeText.length() > 4000 ? resumeText.substring(0, 4000) : resumeText;
      sb.append(" ").append(snippet);
    }

    String result = sb.toString().trim();
    return result.isEmpty() ? "candidate" : result; // Fallback to avoid empty embedding
  }

  /**
   * Generate embeddings in batches to avoid memory issues
   */
  private List<float[]> generateEmbeddingsInBatches(List<String> texts) {
    List<float[]> allEmbeddings = new ArrayList<>();

    for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
      int end = Math.min(start + EMBEDDING_BATCH_SIZE, texts.size());
      List<String> batchTexts = texts.subList(start, end);

      List<float[]> batchEmbeddings = embeddingModel.embed(batchTexts);

      allEmbeddings.addAll(batchEmbeddings);

      log.debug("Generated embeddings for batch {}-{} of {}", start, end, texts.size());
    }

    return allEmbeddings;
  }

  /**
   * Convert Candidate entity to CandidateES document
   */
  private CandidateES convertToCandidateES(Candidate candidate, String resumeText, float[] embedding) {
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
        .resumeText(resumeText)
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

  private String resolveResumeText(String candidateId) {
    return fileRepository
        .findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
            candidateId,
            Status.ACTIVE,
            List.of(FileType.RESUME, FileType.CV))
        .map(File::getResumeExtractedText)
        .filter(StringUtils::hasText)
        .orElse(null);
  }

  /**
   * Determine experience level based on years of experience
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

  /**
   * Clear all documents from CandidateES index
   */
  private void clearIndexData() {
    var indexOps = elasticsearchOperations.indexOps(CandidateES.class);

    if (!indexOps.exists()) {
      log.info("CandidateES index does not exist, skip clearing.");
      return;
    }

    log.info("Clearing all documents in CandidateES Elasticsearch index...");

    elasticsearchOperations.delete(
        org.springframework.data.elasticsearch.core.query.Query.findAll(),
        CandidateES.class);

    indexOps.refresh();
  }
}
