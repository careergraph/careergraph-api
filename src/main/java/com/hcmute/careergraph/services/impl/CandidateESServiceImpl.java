package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.dtos.request.CandidateFilterRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateSuggestionResponse;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.repositories.CandidateESRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.CandidateESService;
import com.hcmute.careergraph.services.EmbedService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateESServiceImpl implements CandidateESService {

  private final CandidateESRepository candidateESRepository;
  private final CandidateRepository candidateRepository;
  private final JobRepository jobRepository;
  private final FileRepository fileRepository;
  private final ElasticsearchClient client;
  private final EmbedService embedService;
  private final ObjectMapper objectMapper;

  /**
   * Hybrid Search: BM25 Text Search + KNN Embedding Search
   * Only returns candidates with isOpenToWork = true
   */
  @Override
  public SearchResponse<CandidateES> hybridSearchCandidates(
      String keyword,
      CandidateFilterRequest filter,
      Pageable pageable) {
    try {
      float[] queryVector = embedService.embed(keyword);

      return client.search(s -> s
          .index("candidates_es")
          .from((int) pageable.getOffset())
          .size(pageable.getPageSize())

          /* ===== HYBRID QUERY: KNN + BM25 trong bool.should ===== */
          .query(q -> q
              .bool(b -> {
                /*
                 * ===== 1. KNN SEMANTIC SEARCH =====
                 * Đặt trong should, boost 0.5 để cân bằng với BM25
                 */
                b.should(sh -> sh
                    .knn(knn -> knn
                        .field("embedding")
                        .queryVector(toFloatList(queryVector))
                        .numCandidates(100)
                        .boost(0.5f)));

                /*
                 * ===== 2. BM25 TEXT SEARCH =====
                 * BestFields: Tìm term match tốt nhất trong 1 field
                 * V2: Thêm cvKeywords^5, giảm resumeText xuống ^1
                 */
                b.should(sh -> sh
                    .multiMatch(mm -> mm
                        .query(keyword)
                        .fields(
                            "desiredPosition^10",    // Intent signal: cao nhất
                            "currentJobTitle^7",     // Evidence: đang làm gì
                            "skills^6",              // Evidence: kỹ năng cụ thể
                            "cvKeywords^9",          // CV keywords/shared CV signal
                            "summary^3",             // Tóm tắt (thường generic)
                            "resumeText^3")          // Raw/fallback CV text
                        .fuzziness("AUTO")
                        .type(TextQueryType.BestFields)
                        .operator(Operator.Or)
                        .minimumShouldMatch("30%")
                        .boost(1.0f)));

                /*
                 * ===== 3. PHRASE PREFIX =====
                 * Boost cao cho exact phrase match
                 * V2: Thêm cvKeywords
                 */
                b.should(sh -> sh
                    .multiMatch(mm -> mm
                        .query(keyword)
                        .fields(
                            "desiredPosition^10",
                            "currentJobTitle^7",
                            "skills^5",
                            "cvKeywords^15",
                            "resumeText^5")
                        .type(TextQueryType.PhrasePrefix)
                        .boost(6.0f)));

                /*
                 * ===== 4. FILTER: isOpenToWork = true =====
                 */
                b.filter(f -> f
                    .term(t -> t
                        .field("isOpenToWork")
                        .value(true)));

                // Ít nhất 1 should clause phải match
                b.minimumShouldMatch("1");

                return b;
              }))

          /* ===== 5. POST FILTER (Additional Filters) ===== */
          .postFilter(pf -> pf
              .bool(b -> {
                applyFilters(b, filter);
                return b;
              })),
          CandidateES.class);

    } catch (Exception e) {
      log.error("Error in hybrid search candidates: {}", e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to execute hybrid search for candidates", e);
    }
  }

  public SearchResponse<CandidateES> hybridSearchCandidatesForCompany(
      List<String> jobSearchTexts,
      CandidateFilterRequest filter,
      Pageable pageable) {
    try {

      return client.search(s -> s
          .index("candidates_es")
          .from((int) pageable.getOffset())
          .size(pageable.getPageSize())
          .query(q -> q.bool(b -> {

            /* ========= OR theo từng JOB (BM25) ========= */
            /* V2: Thêm cvKeywords^5, giảm resumeText xuống ^1 */
            for (String title : jobSearchTexts) {
              b.should(sh -> sh.multiMatch(mm -> mm
                  .query(title)
                  .fields(
                      "desiredPosition^10",
                      "currentJobTitle^7",
                      "skills^6",
                      "cvKeywords^9",
                      "summary^3",
                      "resumeText^3")
                  .type(TextQueryType.BestFields)
                  .operator(Operator.Or)
                  .fuzziness("AUTO")
                  .boost(1.0f)));
            }

            /* ========= OR theo từng JOB (EMBEDDING) ========= */
            for (String title : jobSearchTexts) {
              float[] vector = embedService.embed(title);
              b.should(sh -> sh.knn(knn -> knn
                  .field("embedding")
                  .queryVector(toFloatList(vector))
                  .numCandidates(50)
                  .boost(0.7f)));
            }

            /* ========= FILTER ========= */
            b.filter(f -> f.term(t -> t
                .field("isOpenToWork")
                .value(true)));

            b.minimumShouldMatch("1");
            return b;
          }))
          .postFilter(pf -> pf.bool(b -> {
            applyFilters(b, filter);
            return b;
          })),
          CandidateES.class);

    } catch (Exception e) {
      log.error("Error in hybrid search candidates: {}", e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to execute hybrid search for candidates", e);
    }
  }

  /**
   * Search candidates for company - when no keyword provided
   * Gets job titles from company's active jobs and matches with candidates'
   * desiredPosition
   * V2: Giảm từ 50 → 20 jobs để tránh query quá dài
   */
  @Override
  public SearchResponse<CandidateES> searchCandidatesForCompany(
      String companyId,
      CandidateFilterRequest filter,
      Pageable pageable) {
    try {
      // Get active job titles from company (V2: giảm từ 50 → 20)
        List<Job> companyJobs = jobRepository
          .findActiveJobsByCompanyId(companyId, LocalDate.now().toString(), PageRequest.of(0, 20))
          .getContent();

      if (companyJobs.isEmpty()) {
        log.debug("No active jobs found for company: {}", companyId);
        // Return empty search with just isOpenToWork filter
        return searchWithOnlyOpenToWorkFilter(pageable, filter);
      }

        List<String> jobSearchTexts = companyJobs.stream()
          .map(this::buildJobSearchText)
          .filter(StringUtils::hasText)
          .distinct()
          .toList();

        if (jobSearchTexts.isEmpty()) {
          log.debug("No usable job search text for company: {}", companyId);
          return searchWithOnlyOpenToWorkFilter(pageable, filter);
        }

        log.debug("Searching candidates for active jobs count={}", jobSearchTexts.size());
        return hybridSearchCandidatesForCompany(jobSearchTexts, filter, pageable);

    } catch (Exception e) {
      log.error("Error searching candidates for company {}: {}", companyId, e.getMessage());
      throw new RuntimeException("Failed to search candidates for company", e);
    }
  }

  /**
   * Search with only isOpenToWork filter (no keyword)
   */
  private SearchResponse<CandidateES> searchWithOnlyOpenToWorkFilter(
      Pageable pageable,
      CandidateFilterRequest filter) {
    try {
      return client.search(s -> s
          .index("candidates_es")
          .from((int) pageable.getOffset())
          .size(pageable.getPageSize())
          .query(q -> q
              .bool(b -> {
                b.filter(f -> f
                    .term(t -> t
                        .field("isOpenToWork")
                        .value(true)));
                return b;
              }))
          .postFilter(pf -> pf
              .bool(b -> {
                applyFilters(b, filter);
                return b;
              })),
          CandidateES.class);
    } catch (Exception e) {
      log.error("Error in search with only openToWork filter: {}", e.getMessage());
      throw new RuntimeException("Failed to search candidates", e);
    }
  }

  /**
   * Apply additional filters to the query
   */
  private void applyFilters(
      co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder b,
      CandidateFilterRequest filter) {

    if (filter == null)
      return;

    /* ===== EDUCATION LEVEL ===== */
    if (filter.getEducationLevels() != null && !filter.getEducationLevels().isEmpty()) {
      b.filter(f -> f.terms(t -> t
          .field("educationLevel")
          .terms(v -> v.value(
              filter.getEducationLevels().stream()
                  .map(FieldValue::of)
                  .toList()))));
    }

    /* ===== EXPERIENCE LEVEL ===== */
    if (filter.getExperienceLevels() != null && !filter.getExperienceLevels().isEmpty()) {
      b.filter(f -> f.terms(t -> t
          .field("experienceLevel")
          .terms(v -> v.value(
              filter.getExperienceLevels().stream()
                  .map(FieldValue::of)
                  .toList()))));
    }

    /* ===== INDUSTRIES ===== */
    if (filter.getIndustries() != null && !filter.getIndustries().isEmpty()) {
      b.filter(f -> f.terms(t -> t
          .field("industries")
          .terms(v -> v.value(
              filter.getIndustries().stream()
                  .map(FieldValue::of)
                  .toList()))));
    }

    /* ===== LOCATIONS ===== */
    if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
      b.filter(f -> f.terms(t -> t
          .field("locations")
          .terms(v -> v.value(
              filter.getLocations().stream()
                  .map(FieldValue::of)
                  .toList()))));
    }

    /* ===== WORK TYPES ===== */
    if (filter.getWorkTypes() != null && !filter.getWorkTypes().isEmpty()) {
      b.filter(f -> f.terms(t -> t
          .field("workTypes")
          .terms(v -> v.value(
              filter.getWorkTypes().stream()
                  .map(FieldValue::of)
                  .toList()))));
    }

    /* ===== YEARS OF EXPERIENCE RANGE ===== */
    if (filter.getMinYearsOfExperience() != null || filter.getMaxYearsOfExperience() != null) {
      b.filter(f -> f.range(r -> r
          .number(n -> {
            n.field("yearsOfExperience");
            if (filter.getMinYearsOfExperience() != null) {
              n.gte(filter.getMinYearsOfExperience().doubleValue());
            }
            if (filter.getMaxYearsOfExperience() != null) {
              n.lte(filter.getMaxYearsOfExperience().doubleValue());
            }
            return n;
          })));
    }

    /* ===== SALARY RANGE ===== */
    if (filter.getSalaryMin() != null) {
      b.filter(f -> f.range(r -> r
          .number(n -> n
              .field("salaryExpectationMin")
              .gte(filter.getSalaryMin().doubleValue()))));
    }
    if (filter.getSalaryMax() != null) {
      b.filter(f -> f.range(r -> r
          .number(n -> n
              .field("salaryExpectationMax")
              .lte(filter.getSalaryMax().doubleValue()))));
    }
  }

  @Override
  public CandidateSuggestionResponse toSuggestionResponse(CandidateES candidateES, Float score) {
    ResumeProjection resume = resolveResponseResume(candidateES);

    return CandidateSuggestionResponse.builder()
        .id(candidateES.getId())
        .firstName(candidateES.getFirstName())
        .lastName(candidateES.getLastName())
        .email(candidateES.getEmail())
        .phone(candidateES.getPhone())
        .avatar(candidateES.getAvatar())
        .gender(candidateES.getGender())
        .desiredPosition(candidateES.getDesiredPosition())
        .currentJobTitle(candidateES.getCurrentJobTitle())
        .yearsOfExperience(candidateES.getYearsOfExperience())
        .experienceLevel(candidateES.getExperienceLevel())
        .educationLevel(candidateES.getEducationLevel())
        .industries(candidateES.getIndustries())
        .locations(candidateES.getLocations())
        .workTypes(candidateES.getWorkTypes())
        .salaryExpectationMin(candidateES.getSalaryExpectationMin())
        .salaryExpectationMax(candidateES.getSalaryExpectationMax())
        .skills(candidateES.getSkills())
        .summary(candidateES.getSummary())
        .isOpenToWork(candidateES.getIsOpenToWork())
        .lastActive(candidateES.getLastActive())
        .resumeFileId(resume.fileId())
        .resumeFileName(resume.fileName())
        .resumeUrl(resume.url())
        .profileUrl("/candidates?candidateId=" + candidateES.getId())
        .score(score)
        .build();
  }

  private ResumeProjection resolveResponseResume(CandidateES candidateES) {
    if (candidateES == null) {
      return ResumeProjection.empty();
    }

    if (StringUtils.hasText(candidateES.getResumeFileId())
        && StringUtils.hasText(candidateES.getResumeUrl())
        && StringUtils.hasText(candidateES.getResumeFileName())) {
      return new ResumeProjection(
          candidateES.getResumeText(),
          candidateES.getCvKeywords(),
          candidateES.getResumeFileId(),
          candidateES.getResumeFileName(),
          candidateES.getResumeUrl(),
          candidateES.getResumeUpdatedAt(),
          candidateES.getResumeContentHash());
    }

    if (StringUtils.hasText(candidateES.getResumeFileId())) {
      Optional<File> file = fileRepository.findById(candidateES.getResumeFileId())
          .filter(item -> candidateES.getId().equals(item.getOwnerId()))
          .filter(item -> Status.ACTIVE.equals(item.getStatus()))
          .filter(item -> Boolean.TRUE.equals(item.getShareToFindJob()));

      if (file.isPresent()) {
        File resume = file.get();
        return new ResumeProjection(
            candidateES.getResumeText(),
            candidateES.getCvKeywords(),
            resume.getId(),
            resolveResumeFileName(resume),
            resume.getFilePath(),
            resume.getLastModifiedDate() != null ? resume.getLastModifiedDate() : resume.getCreatedDate(),
            resume.getResumeContentHash());
      }
    }

    ResumeProjection sharedResume = resolveSharedResume(candidateES.getId());
    if (StringUtils.hasText(sharedResume.fileId())) {
      return sharedResume;
    }

    return new ResumeProjection(
        candidateES.getResumeText(),
        candidateES.getCvKeywords(),
        candidateES.getResumeFileId(),
        candidateES.getResumeFileName(),
        candidateES.getResumeUrl(),
        candidateES.getResumeUpdatedAt(),
        candidateES.getResumeContentHash());
  }

  @Override
  public CandidateES indexCandidate(Candidate candidate) {
    try {
      if (!shouldIndexCandidate(candidate)) {
        deleteCandidate(candidate.getId());
        log.info("Skipped indexing candidate {} because the profile is not searchable: {}",
            candidate.getId(), explainNotSearchable(candidate));
        return null;
      }

      CandidateES candidateES = mapToES(candidate);

      // Generate embedding from search text
      String searchText = candidateES.buildSearchText();
      if (searchText != null && !searchText.isEmpty()) {
        float[] embedding = embedService.embed(searchText);
        candidateES.setEmbedding(embedding);
      }

      return candidateESRepository.save(candidateES);
    } catch (Exception e) {
      log.error("Error indexing candidate {}: {}", candidate.getId(), e.getMessage());
      throw new RuntimeException("Failed to index candidate", e);
    }
  }

  @Override
  public void deleteCandidate(String candidateId) {
    try {
      candidateESRepository.deleteById(candidateId);
      log.info("Deleted candidate from ES: {}", candidateId);
    } catch (Exception e) {
      log.error("Error deleting candidate {}: {}", candidateId, e.getMessage());
    }
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public int syncAllCandidates() {
    log.info("Starting sync all candidates to Elasticsearch...");
    List<String> candidateIds = candidateRepository.findAllIds();
    int count = 0;

    for (String candidateId : candidateIds) {
      try {
        Candidate candidate = candidateRepository.findByIdWithCollections(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
        CandidateES indexed = indexCandidate(candidate);
        if (indexed != null) {
          count++;
        }
      } catch (Exception e) {
        log.error("Failed to index candidate {}: {}", candidateId, e.getMessage());
      }
    }

    log.info("Synced {} candidates to Elasticsearch", count);
    return count;
  }

  @Override
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public void syncCandidate(String candidateId) {
    log.info("Syncing candidate {} to Elasticsearch", candidateId);
    
    try {
      // V2.1: Fetch với eager loading để tránh LazyInitializationException trong async context
      Candidate candidate = candidateRepository.findByIdWithCollections(candidateId)
          .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
      
      indexCandidate(candidate);
      log.info("Successfully synced candidate {} to Elasticsearch", candidateId);
    } catch (Exception e) {
      log.error("Failed to sync candidate {} to Elasticsearch: {}", candidateId, e.getMessage());
      throw new RuntimeException("Failed to sync candidate to Elasticsearch", e);
    }
  }

  /**
   * Map JPA Candidate entity to CandidateES document
   * V2: Thêm cvKeywords extraction từ File.cvKeywordsJson
   */
  private CandidateES mapToES(Candidate candidate) {
    List<String> skillNames = new ArrayList<>();
    if (candidate.getSkills() != null) {
      skillNames = candidate.getSkills().stream()
          .map(cs -> cs.getSkill() != null ? cs.getSkill().getName() : null)
          .filter(name -> name != null)
          .toList();
    }

    // Extract phone from contacts
    String phone = null;
    if (candidate.getContacts() != null) {
      phone = candidate.getContacts().stream()
          .filter(c -> c.getContactType() != null && "PHONE".equals(c.getContactType().name()))
          .map(c -> c.getValue())
          .findFirst()
          .orElse(null);
    }

    // Get email from account
    String email = null;
    if (candidate.getAccount() != null) {
      email = candidate.getAccount().getEmail();
    }

    ResumeProjection resume = resolveSharedResume(candidate.getId());

    return CandidateES.builder()
        .id(candidate.getId())
        .firstName(candidate.getFirstName())
        .lastName(candidate.getLastName())
        .email(email)
        .phone(phone)
        .avatar(candidate.getAvatar())
        .gender(candidate.getGender())
        .yearsOfExperience(candidate.getYearsOfExperience())
        .desiredPosition(candidate.getDesiredPosition())
        .currentJobTitle(candidate.getCurrentJobTitle())
        .summary(candidate.getSummary())
        .resumeText(resume.resumeText())
        .cvKeywords(resume.cvKeywords())  // V2: CV keywords từ extraction service
        .resumeFileId(resume.fileId())
        .resumeFileName(resume.fileName())
        .resumeUrl(resume.url())
        .resumeUpdatedAt(resume.updatedAt())
        .resumeContentHash(resume.contentHash())
        .isOpenToWork(candidate.getIsOpenToWork() != null ? candidate.getIsOpenToWork() : false)
        .educationLevel(candidate.getEducationLevel())
        .experienceLevel(determineExperienceLevel(candidate.getYearsOfExperience()))
        .industries(candidate.getIndustries())
        .locations(candidate.getLocations())
        .workTypes(candidate.getWorkTypes())
        .salaryExpectationMin(candidate.getSalaryExpectationMin())
        .salaryExpectationMax(candidate.getSalaryExpectationMax())
        .skills(skillNames)
        .createdAt(LocalDate.now())
        .lastActive(LocalDate.now())
        .contentHash(buildContentHash(candidate, resume, skillNames))
        .build();
  }

  /**
   * V2.1: Resolve ALL resumes với shareToFindJob = true
   * Aggregate tất cả CV text & keywords để candidate search dựa vào cả tiêu chí + CV
   */
  private ResumeProjection resolveResume(String candidateId) {
    List<File> activeResumes = fileRepository
        .findByOwnerIdAndStatusAndFileTypeInAndShareToFindJobOrderByCreatedDateDesc(
            candidateId,
            Status.ACTIVE,
            List.of(FileType.RESUME, FileType.CV),
            true)  // Chỉ lấy CV đang bật tìm việc
        .stream()
        .filter(file -> StringUtils.hasText(file.getResumeExtractedText()))
        .toList();
    
    if (activeResumes.isEmpty()) {
      return ResumeProjection.empty();
    }
    
    // Aggregate all CV texts (limit mỗi CV 1000 chars để tránh quá dài)
    StringBuilder aggregatedText = new StringBuilder();
    StringBuilder aggregatedKeywords = new StringBuilder();
    String latestFileId = null;
    java.time.LocalDateTime latestUpdatedAt = null;
    String latestContentHash = null;
    
    for (File file : activeResumes) {
      // Resume text (limit 1000 chars per CV)
      if (StringUtils.hasText(file.getResumeExtractedText())) {
        String snippet = file.getResumeExtractedText().length() > 1000 
            ? file.getResumeExtractedText().substring(0, 1000) 
            : file.getResumeExtractedText();
        aggregatedText.append(snippet).append(" ");
      }
      
      // CV keywords
      String keywords = extractCvKeywords(file);
      if (StringUtils.hasText(keywords)) {
        aggregatedKeywords.append(keywords).append(" ");
      }
      
      // Track latest file metadata
      if (latestFileId == null) {
        latestFileId = file.getId();
        latestUpdatedAt = file.getLastModifiedDate() != null 
            ? file.getLastModifiedDate() 
            : file.getCreatedDate();
        latestContentHash = file.getResumeContentHash();
      }
    }
    
    return new ResumeProjection(
        aggregatedText.toString().trim(),
        aggregatedKeywords.toString().trim(),
        latestFileId,
        null,
        null,
        latestUpdatedAt,
        latestContentHash);
  }
  
  /**
   * V2: Extract CV keywords từ File.cvKeywordsJson
   * Parse JSON và lấy searchKeywords field
   */
  private ResumeProjection resolveSharedResume(String candidateId) {
    Optional<File> sharedResume = fileRepository
        .findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
            candidateId,
            Status.ACTIVE,
            List.of(FileType.RESUME, FileType.CV));

    if (sharedResume.isEmpty()) {
      return ResumeProjection.empty();
    }

    File file = sharedResume.get();
    String resumeText = null;
    String metadataFallback = buildResumeMetadataFallback(file);
    if (StringUtils.hasText(file.getResumeExtractedText())) {
      resumeText = file.getResumeExtractedText().length() > 1000
          ? file.getResumeExtractedText().substring(0, 1000)
          : file.getResumeExtractedText();
    } else {
      resumeText = metadataFallback;
    }

    String cvKeywords = extractCvKeywords(file);
    if (!StringUtils.hasText(cvKeywords)) {
      cvKeywords = metadataFallback;
    }

    return new ResumeProjection(
        resumeText,
        cvKeywords,
        file.getId(),
        resolveResumeFileName(file),
        file.getFilePath(),
        file.getLastModifiedDate() != null ? file.getLastModifiedDate() : file.getCreatedDate(),
        file.getResumeContentHash());
  }

  private String resolveResumeFileName(File file) {
    if (file == null) {
      return null;
    }
    if (StringUtils.hasText(file.getOriginalFileName())) {
      return file.getOriginalFileName();
    }
    return file.getFileName();
  }

  private String buildResumeMetadataFallback(File file) {
    if (file == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    if (StringUtils.hasText(file.getFileName())) {
      sb.append(file.getFileName()).append(' ');
    }
    if (StringUtils.hasText(file.getOriginalFileName())
        && !file.getOriginalFileName().equals(file.getFileName())) {
      sb.append(file.getOriginalFileName()).append(' ');
    }
    String value = sb.toString()
        .replaceAll("([a-z])([A-Z])", "$1 $2")
        .replaceAll("([A-Za-z])([0-9])", "$1 $2")
        .replaceAll("([0-9])([A-Za-z])", "$1 $2")
        .replaceAll("[._\\-]+", " ")
        .replaceAll("(?i)\\b(pdf|docx?|resume|cv)\\b", " ")
        .replaceAll("\\s+", " ")
        .trim();
    if (!StringUtils.hasText(value)) {
      return null;
    }

    String[] tokens = value.split("\\s+");
    if (tokens.length >= 2) {
      String tail = tokens[tokens.length - 2] + " " + tokens[tokens.length - 1];
      return (tail + " " + tail + " " + value).trim();
    }
    return value;
  }

  private String extractCvKeywords(File file) {
    if (file == null || !StringUtils.hasText(file.getCvKeywordsJson())) {
      return null;
    }
    
    try {
      JsonNode node = objectMapper.readTree(file.getCvKeywordsJson());
      JsonNode searchKeywords = node.get("searchKeywords");
      if (searchKeywords != null && !searchKeywords.isNull()) {
        return searchKeywords.asText("");
      }
    } catch (Exception e) {
      log.warn("Failed to parse cvKeywordsJson for file {}: {}", file.getId(), e.getMessage());
    }
    
    return null;
  }

  private record ResumeProjection(
      String resumeText, 
      String cvKeywords,  // V2: Thêm field
      String fileId, 
      String fileName,
      String url,
      java.time.LocalDateTime updatedAt,
      String contentHash) {
    static ResumeProjection empty() {
      return new ResumeProjection(null, null, null, null, null, null, null);
    }
  }

  /**
   * V2: Build compact job search text - chỉ title + top qualifications
   * Giảm từ 2000-5000 chars xuống ~200-400 chars để BM25 scoring chính xác hơn
   */
  private boolean shouldIndexCandidate(Candidate candidate) {
    if (candidate == null || !Boolean.TRUE.equals(candidate.getIsOpenToWork())) {
      return false;
    }

    if (hasCandidateIntent(candidate)) {
      return true;
    }

    ResumeProjection sharedResume = resolveSharedResume(candidate.getId());
    return StringUtils.hasText(sharedResume.cvKeywords()) || StringUtils.hasText(sharedResume.resumeText());
  }

  private String explainNotSearchable(Candidate candidate) {
    if (candidate == null) {
      return "candidate is null";
    }
    if (!Boolean.TRUE.equals(candidate.getIsOpenToWork())) {
      return "isOpenToWork is false";
    }
    if (hasCandidateIntent(candidate)) {
      return "unexpected state: candidate has profile intent";
    }

    ResumeProjection sharedResume = resolveSharedResume(candidate.getId());
    if (!StringUtils.hasText(sharedResume.fileId())) {
      return "no profile intent and no active shared CV";
    }
    return "shared CV " + sharedResume.fileId() + " has no extracted text or keywords yet";
  }

  private boolean hasCandidateIntent(Candidate candidate) {
    if (StringUtils.hasText(candidate.getDesiredPosition())
        || StringUtils.hasText(candidate.getCurrentJobTitle())
        || StringUtils.hasText(candidate.getSummary())) {
      return true;
    }

    return candidate.getSkills() != null && candidate.getSkills().stream()
        .anyMatch(cs -> cs.getSkill() != null && StringUtils.hasText(cs.getSkill().getName()));
  }

  private String buildContentHash(Candidate candidate, ResumeProjection resume, List<String> skillNames) {
    String content = String.join("|",
        safe(candidate.getDesiredPosition()),
        safe(candidate.getCurrentJobTitle()),
        safe(candidate.getSummary()),
        safe(candidate.getEducationLevel()),
        String.valueOf(candidate.getYearsOfExperience()),
        String.join(",", safeList(candidate.getIndustries())),
        String.join(",", safeList(candidate.getLocations())),
        String.join(",", safeList(candidate.getWorkTypes())),
        String.join(",", safeList(skillNames)),
        safe(resume.cvKeywords()),
        safe(resume.contentHash()));
    return sha256(content);
  }

  private List<String> safeList(List<String> values) {
    return values == null ? List.of() : values;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private String sha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot calculate candidate content hash", e);
    }
  }

  private String determineExperienceLevel(Integer yearsOfExperience) {
    if (yearsOfExperience == null || yearsOfExperience == 0) {
      return "FRESHER";
    }
    if (yearsOfExperience <= 2) {
      return "JUNIOR";
    }
    if (yearsOfExperience <= 5) {
      return "MIDDLE";
    }
    if (yearsOfExperience <= 10) {
      return "SENIOR";
    }
    return "EXPERT";
  }

  private String buildJobSearchText(Job job) {
    if (job == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    
    // CHỈ dùng title + top qualifications (không description full)
    if (StringUtils.hasText(job.getTitle())) {
      sb.append(job.getTitle()).append(" ");
    }
    
    // Top 3 qualifications/requirements (thường chứa tech keywords)
    appendTopLines(sb, job.getMinimumQualifications(), 3);
    appendTopLines(sb, job.getQualifications(), 3);
    
    return sb.toString().trim();
  }
  
  /**
   * V2: Append only top N lines from a list
   */
  private void appendTopLines(StringBuilder sb, List<String> lines, int maxLines) {
    if (lines == null || lines.isEmpty()) {
      return;
    }
    int count = 0;
    for (String line : lines) {
      if (count >= maxLines) break;
      if (StringUtils.hasText(line)) {
        sb.append(line).append(" ");
        count++;
      }
    }
  }
  
  /**
   * V2: Normalize ES score thành % match (0-100).
   * Dùng min-max normalization trên batch results.
   * 
   * @param rawScore ES raw score
   * @param maxScore Max score trong batch results
   * @return Normalized score 0-100
   */
  private float normalizeScore(float rawScore, float maxScore) {
    if (maxScore <= 0) return 0f;
    
    // Min-max normalization: (score / maxScore) * 100
    float normalized = (rawScore / maxScore) * 100f;
    return Math.min(normalized, 100f);
  }

  private List<Float> toFloatList(float[] vector) {
    List<Float> list = new ArrayList<>();
    for (float v : vector) {
      list.add(v);
    }
    return list;
  }
}
