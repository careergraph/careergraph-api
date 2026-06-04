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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                            "cvKeywords^5",          // CV keywords (NEW, sạch hơn resumeText)
                            "summary^3",             // Tóm tắt (thường generic)
                            "resumeText^1")          // Raw CV (giảm từ ^2, chỉ backup)
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
                            "cvKeywords^4",
                            "resumeText^1.5")
                        .type(TextQueryType.PhrasePrefix)
                        .boost(1.5f)));

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
                      "cvKeywords^5",       // NEW: CV keywords
                      "summary^3",
                      "resumeText^1")       // Giảm từ ^2
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
        .score(score)
        .build();
  }

  @Override
  public CandidateES indexCandidate(Candidate candidate) {
    try {
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
  public int syncAllCandidates() {
    log.info("Starting sync all candidates to Elasticsearch...");
    List<Candidate> candidates = candidateRepository.findAll();
    int count = 0;

    for (Candidate candidate : candidates) {
      try {
        indexCandidate(candidate);
        count++;
      } catch (Exception e) {
        log.error("Failed to index candidate {}: {}", candidate.getId(), e.getMessage());
      }
    }

    log.info("Synced {} candidates to Elasticsearch", count);
    return count;
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
          .filter(c -> "PHONE".equals(c.getContactType()))
          .map(c -> c.getValue())
          .findFirst()
          .orElse(null);
    }

    // Get email from account
    String email = null;
    if (candidate.getAccount() != null) {
      email = candidate.getAccount().getEmail();
    }

    ResumeProjection resume = resolveResume(candidate.getId());

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
        .resumeUpdatedAt(resume.updatedAt())
        .resumeContentHash(resume.contentHash())
        .isOpenToWork(candidate.getIsOpenToWork() != null ? candidate.getIsOpenToWork() : false)
        .educationLevel(candidate.getEducationLevel())
        .industries(candidate.getIndustries())
        .locations(candidate.getLocations())
        .workTypes(candidate.getWorkTypes())
        .salaryExpectationMin(candidate.getSalaryExpectationMin())
        .salaryExpectationMax(candidate.getSalaryExpectationMax())
        .skills(skillNames)
        .createdAt(LocalDate.now())
        .lastActive(LocalDate.now())
        .build();
  }

  /**
   * V2: Resolve resume projection với cvKeywords
   * Không filter shareToFindJob - dùng CV mới nhất active
   */
  private ResumeProjection resolveResume(String candidateId) {
    return fileRepository
        .findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
            candidateId,
            Status.ACTIVE,
            List.of(FileType.RESUME, FileType.CV))
        .filter(file -> StringUtils.hasText(file.getResumeExtractedText()))
        .map(file -> {
          String cvKeywords = extractCvKeywords(file);
          return new ResumeProjection(
              file.getResumeExtractedText(),
              cvKeywords,
              file.getId(),
              file.getLastModifiedDate() != null ? file.getLastModifiedDate() : file.getCreatedDate(),
              file.getResumeContentHash());
        })
        .orElse(ResumeProjection.empty());
  }
  
  /**
   * V2: Extract CV keywords từ File.cvKeywordsJson
   * Parse JSON và lấy searchKeywords field
   */
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
      java.time.LocalDateTime updatedAt,
      String contentHash) {
    static ResumeProjection empty() {
      return new ResumeProjection(null, null, null, null, null);
    }
  }

  /**
   * V2: Build compact job search text - chỉ title + top qualifications
   * Giảm từ 2000-5000 chars xuống ~200-400 chars để BM25 scoring chính xác hơn
   */
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
