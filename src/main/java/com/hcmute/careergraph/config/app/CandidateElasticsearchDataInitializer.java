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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

  private record CandidateIndexPayload(
    String id,
    String firstName,
    String lastName,
    String email,
    String gender,
    Integer yearsOfExperience,
    String desiredPosition,
    String currentJobTitle,
    String summary,
    Boolean openToWork,
    String educationLevel,
    List<String> industries,
    List<String> locations,
    List<String> workTypes,
    Integer salaryExpectationMin,
    Integer salaryExpectationMax,
    List<String> skills,
    LocalDate createdAt,
    String resumeText) {
  }

  private static final String LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE = "Local candidate embedding service unavailable and Gemini fallback is disabled.";

  private final CandidateRepository candidateRepository;
  private final CandidateESRepository candidateESRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final FileRepository fileRepository;
  private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;
  private final EmbeddingModel embeddingModel;

  @Value("${APP_ES_SYNC_CANDIDATES_ENABLED:false}")
  private boolean syncCandidatesEnabled;

  @Value("${APP_ES_FORCE_CANDIDATES_FULL_SYNC:false}")
  private boolean forceFullSync;

  @Value("${APP_ES_ALLOW_GEMINI_FALLBACK:false}")
  private boolean allowGeminiFallback;

  @Value("${APP_ES_MAX_CANDIDATE_EMBEDDINGS_PER_RUN:30}")
  private int maxEmbeddingsPerRun;

  private static final int EMBEDDING_BATCH_SIZE = 100;
  private static final int MAX_RETRIES = 5;
  private static final long DELAY_SECONDS = 10;

  @Override
  public void run(String... args) throws Exception {
    if (!syncCandidatesEnabled) {
      log.info("Skip Candidate Elasticsearch synchronization because APP_ES_SYNC_CANDIDATES_ENABLED=false");
      return;
    }
    syncNow(null, null);
  }

  public ElasticsearchSyncResult syncNow(Boolean forceOverride, Integer maxEmbeddingsOverride) {
    boolean effectiveForce = forceOverride != null ? forceOverride : forceFullSync;
    int effectiveMaxEmbeddings = maxEmbeddingsOverride != null ? maxEmbeddingsOverride : maxEmbeddingsPerRun;

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
          return new ElasticsearchSyncResult("candidates", true, effectiveForce, effectiveMaxEmbeddings, 0, 0, 0,
            "No candidates found in database.");
        }

        List<CandidateIndexPayload> payloads = allCandidates.stream()
          .map(this::toPayload)
          .toList();

        long indexedCandidatesCount = candidateESRepository.count();
        List<Integer> candidateIndexesToEmbed = new ArrayList<>();

        for (int i = 0; i < payloads.size(); i++) {
          CandidateIndexPayload payload = payloads.get(i);
          String searchText = buildSearchTextFromPayload(payload);
          String contentHash = hashText(searchText);
          CandidateES existing = candidateESRepository.findById(payload.id()).orElse(null);

          if (!effectiveForce && existing != null && contentHash.equals(existing.getContentHash())) {
            continue;
          }

          candidateIndexesToEmbed.add(i);
        }

        if (candidateIndexesToEmbed.isEmpty() && indexedCandidatesCount >= allCandidates.size()) {
          log.info("Skip Candidate Elasticsearch synchronization because no candidates changed since the last index run.");
          return new ElasticsearchSyncResult(
            "candidates",
            true,
            effectiveForce,
            effectiveMaxEmbeddings,
            0,
            allCandidates.size(),
            0,
            "No candidates changed since the last index run.");
        }

        int cappedCount = Math.min(candidateIndexesToEmbed.size(), Math.max(1, effectiveMaxEmbeddings));
        List<Integer> indexesToEmbed = candidateIndexesToEmbed.subList(0, cappedCount);

        List<String> searchTexts = indexesToEmbed.stream()
          .map(i -> buildSearchTextFromPayload(payloads.get(i)))
          .toList();

        List<float[]> embeddings = generateEmbeddingsInBatches(searchTexts);

        if (effectiveForce) {
          clearIndexData();
        }

        List<CandidateES> candidatesToSave = IntStream.range(0, indexesToEmbed.size())
          .mapToObj(i -> convertToCandidateES(payloads.get(indexesToEmbed.get(i)), embeddings.get(i)))
          .toList();

        candidateESRepository.saveAll(candidatesToSave);

        log.info("✅ Candidate Elasticsearch synchronization complete. Indexed {} changed candidates in this run, pending {} more changed candidates.",
          candidatesToSave.size(),
          Math.max(0, candidateIndexesToEmbed.size() - candidatesToSave.size()));
        return new ElasticsearchSyncResult(
          "candidates",
          false,
          effectiveForce,
          effectiveMaxEmbeddings,
          candidatesToSave.size(),
          Math.max(0, allCandidates.size() - candidateIndexesToEmbed.size()),
          Math.max(0, candidateIndexesToEmbed.size() - candidatesToSave.size()),
          "Candidate Elasticsearch synchronization completed.");

      } catch (Exception e) {
        if (LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE.equals(e.getMessage())) {
          log.warn("{} Skipping Candidate Elasticsearch synchronization.", LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE);
          return new ElasticsearchSyncResult(
            "candidates",
            true,
            effectiveForce,
            effectiveMaxEmbeddings,
            0,
            0,
            0,
            LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE);
        }

        log.error("Attempt {} failed. Reason: {}", attempt, e.getMessage());

        if (attempt < MAX_RETRIES) {
          log.info("Retrying in {} seconds...", DELAY_SECONDS);
          try {
            TimeUnit.SECONDS.sleep(DELAY_SECONDS);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Synchronization thread was interrupted during delay.");
            return new ElasticsearchSyncResult(
              "candidates",
              true,
              effectiveForce,
              effectiveMaxEmbeddings,
              0,
              0,
              0,
              "Synchronization thread was interrupted during delay.");
          }
        } else {
          log.error("Failed to synchronize Candidate data to Elasticsearch after {} attempts.", MAX_RETRIES);
          return new ElasticsearchSyncResult(
            "candidates",
            true,
            effectiveForce,
            effectiveMaxEmbeddings,
            0,
            0,
            0,
            "Failed to synchronize Candidate data to Elasticsearch after retries: " + e.getMessage());
        }
      }
    }

    return new ElasticsearchSyncResult(
      "candidates",
      true,
      effectiveForce,
      effectiveMaxEmbeddings,
      0,
      0,
      0,
      "Candidate Elasticsearch synchronization finished without processing.");
  }

  /**
   * Build search text from Candidate entity for embedding generation
   */
  private CandidateIndexPayload toPayload(Candidate candidate) {
    List<String> skillNames = new ArrayList<>();
    if (candidate.getSkills() != null) {
      skillNames = candidate.getSkills().stream()
        .filter(cs -> cs.getSkill() != null && cs.getSkill().getName() != null)
        .map(cs -> cs.getSkill().getName())
        .toList();
    }

    String email = null;
    if (candidate.getAccount() != null) {
      email = candidate.getAccount().getEmail();
    }

    return new CandidateIndexPayload(
      candidate.getId(),
      candidate.getFirstName(),
      candidate.getLastName(),
      email,
      candidate.getGender(),
      candidate.getYearsOfExperience(),
      candidate.getDesiredPosition(),
      candidate.getCurrentJobTitle(),
      candidate.getSummary(),
      candidate.getIsOpenToWork(),
      candidate.getEducationLevel(),
      candidate.getIndustries(),
      candidate.getLocations(),
      candidate.getWorkTypes(),
      candidate.getSalaryExpectationMin(),
      candidate.getSalaryExpectationMax(),
      skillNames,
      candidate.getCreatedDate() != null ? candidate.getCreatedDate().toLocalDate() : LocalDate.now(),
      resolveResumeText(candidate.getId()));
  }

  private String buildSearchTextFromPayload(CandidateIndexPayload payload) {
    StringBuilder sb = new StringBuilder();

    if (payload.desiredPosition() != null) {
      sb.append(payload.desiredPosition()).append(" ");
    }
    if (payload.currentJobTitle() != null) {
      sb.append(payload.currentJobTitle()).append(" ");
    }
    if (payload.skills() != null && !payload.skills().isEmpty()) {
      String skillNames = payload.skills().stream().collect(Collectors.joining(" "));
      sb.append(skillNames).append(" ");
    }
    if (payload.summary() != null) {
      sb.append(payload.summary());
    }
    if (StringUtils.hasText(payload.resumeText())) {
      String snippet = payload.resumeText().length() > 4000 ? payload.resumeText().substring(0, 4000) : payload.resumeText();
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

      List<float[]> batchEmbeddings = embedBatch(batchTexts);

      allEmbeddings.addAll(batchEmbeddings);

      log.debug("Generated embeddings for batch {}-{} of {}", start, end, texts.size());
    }

    return allEmbeddings;
  }

  private List<float[]> embedBatch(List<String> batchTexts) {
    try {
      return huggingFaceEmbeddingService.embed(batchTexts);
    } catch (Exception ex) {
      if (!allowGeminiFallback) {
        throw new IllegalStateException(LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE, ex);
      }
      log.warn("Local candidate embedding service unavailable, falling back to configured Spring AI embedding model: {}",
        ex.getMessage());
      return embeddingModel.embed(batchTexts);
    }
  }

  /**
   * Convert Candidate entity to CandidateES document
   */
  private CandidateES convertToCandidateES(CandidateIndexPayload payload, float[] embedding) {
    return CandidateES.builder()
        .id(payload.id())
        .firstName(payload.firstName())
        .lastName(payload.lastName())
        .email(payload.email())
        .gender(payload.gender())
        .yearsOfExperience(payload.yearsOfExperience())
        .desiredPosition(payload.desiredPosition())
        .currentJobTitle(payload.currentJobTitle())
        .summary(payload.summary())
        .resumeText(payload.resumeText())
        .isOpenToWork(payload.openToWork() != null ? payload.openToWork() : false)
        .educationLevel(payload.educationLevel())
        .experienceLevel(determineExperienceLevel(payload.yearsOfExperience()))
        .industries(payload.industries())
        .locations(payload.locations())
        .workTypes(payload.workTypes())
        .salaryExpectationMin(payload.salaryExpectationMin())
        .salaryExpectationMax(payload.salaryExpectationMax())
        .skills(payload.skills())
        .createdAt(payload.createdAt())
        .lastActive(LocalDate.now())
        .contentHash(hashText(buildSearchTextFromPayload(payload)))
        .embedding(embedding)
        .build();
  }

  private String hashText(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte value : hash) {
        builder.append(String.format(Locale.ROOT, "%02x", value));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm is not available", ex);
    }
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
