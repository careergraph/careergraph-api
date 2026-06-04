# Phase 1: Configuration Management & Combined KNN

**Phase:** 1 of 3  
**Duration:** 2 weeks (80 hours Senior Dev + 40 hours DevOps + 40 hours QA)  
**Priority:** 🔴 HIGH (Foundation cho Phase 2 & 3)  
**Status:** 📋 PLANNING  

---

## 1. Context & Business Case

### 1.1 Current Pain Points

**Problem 1: Hard-coded Constants**
```java
// Scattered across codebase
private static final int MAX_JOBS = 20;        // CandidateESServiceImpl.java:220
private static final int SNIPPET_LENGTH = 500;  // CandidateES.java:153
private static final int TOP_QUALIFICATIONS = 3; // CandidateESServiceImpl.java:585
```

**Impact:**
- Không thể tune performance cho different company sizes
- Deploy cần rebuild application
- Testing khó (không thể A/B test với different values)

**Problem 2: N Separate KNN Queries**
```java
// Current V2 implementation
for (String jobSearchText : jobSearchTexts) {  // 20 iterations
    float[] vector = embedService.embed(jobSearchText);  // 20 API calls
    b.should(sh -> sh.knn(...));  // 20 KNN queries
}
```

**Impact:**
- Search latency: ~1.5s cho 20 jobs (N × 75ms)
- Elasticsearch load: 20× queries
- Embedding API cost: 20× calls

### 1.2 Business Goals

1. **Reduce search latency < 500ms** (67% improvement)
2. **Config-driven tuning** → enable rapid optimization
3. **Reduce ES query load** → lower infrastructure cost

### 1.3 Success Metrics

| Metric | Current (V2) | Target (V3 Phase 1) |
|--------|--------------|---------------------|
| P95 latency (no keyword) | 1500ms | < 500ms |
| ES queries per search | 20 KNN + 20 BM25 | 1 KNN + 1 BM25 |
| Embedding API calls | 20/search | 1/search |
| Config deployment time | 30 min (rebuild) | < 1 min (reload) |

---

## 2. Technical Design

### 2.1 Configuration Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Configuration Layer                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  application.yml (Git)                                       │
│         ↓                                                    │
│  ConfigurationProperties (@RefreshScope)                     │
│         ↓                                                    │
│  HrSearchConfig (Validated Bean)                            │
│         ↓                                                    │
│  [CandidateESServiceImpl, CandidateES, etc.]                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Components:**

1. **HrSearchConfig.java** — Centralized config bean
2. **application.yml** — Config source
3. **Validation** — JSR-303 Bean Validation
4. **Hot Reload** — Spring Cloud Config (optional) hoặc `@RefreshScope`

### 2.2 Combined Embedding Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Combined Embedding Strategy                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Input: List<Job> (20 jobs)                                 │
│         ↓                                                    │
│  Extract: List<String> jobSearchTexts                       │
│         ↓                                                    │
│  Combine Strategy:                                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Option A: Simple Concatenation                        │  │
│  │   "Frontend Dev React ... Backend Java Spring ..."   │  │
│  │   → May exceed embedding model max length (512 tokens) │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ Option B: Weighted Sampling (RECOMMENDED)             │  │
│  │   - Sample top 5 jobs by recency                      │  │
│  │   - Extract key terms (TF-IDF)                        │  │
│  │   - Build representative text (~300 chars)            │  │
│  └──────────────────────────────────────────────────────┘  │
│         ↓                                                    │
│  embedService.embed(combinedText)  // 1 call                │
│         ↓                                                    │
│  Single KNN query to Elasticsearch                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Implementation Tasks

### 3.1 Task Breakdown (Senior Backend Developer)

#### Task 1.1: Create Configuration Infrastructure (8 hours)

**Role:** Senior Backend Developer  
**Files to create:**
- `config/HrSearchConfig.java`
- `config/HrSearchConfigValidator.java`

**Implementation:**

```java
package com.hcmute.careergraph.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;

/**
 * Centralized configuration for HR Search V3
 * All previously hard-coded constants moved here
 */
@Configuration
@ConfigurationProperties(prefix = "application.hr-search")
@Validated
@Data
public class HrSearchConfig {
    
    /**
     * Maximum number of jobs to consider when HR doesn't provide keyword
     * V2: Hard-coded 20
     * V3: Configurable, default 20
     */
    @Min(1)
    @Max(50)
    private int maxJobsForSearch = 20;
    
    /**
     * Length of resume text snippet for fallback embedding
     * V2: Hard-coded 500
     * V3: Configurable, default 500
     */
    @Min(100)
    @Max(2000)
    private int embeddingSnippetLength = 500;
    
    /**
     * Number of top qualifications to extract from job description
     * V2: Hard-coded 3
     * V3: Configurable, default 3
     */
    @Min(1)
    @Max(10)
    private int topQualificationsCount = 3;
    
    /**
     * Enable combined KNN strategy (Phase 1)
     * true: Use 1 combined embedding for all jobs
     * false: Use N separate embeddings (V2 behavior)
     */
    private boolean combinedKnnEnabled = true;
    
    /**
     * Sampling strategy for combined embedding
     * TOP_N: Sample top N recent jobs
     * TF_IDF: Extract key terms via TF-IDF
     * WEIGHTED: Weighted combination of both
     */
    private SamplingStrategy combinedKnnSamplingStrategy = SamplingStrategy.TOP_N;
    
    /**
     * Number of jobs to sample for combined embedding
     */
    @Min(3)
    @Max(20)
    private int combinedKnnSampleSize = 5;
    
    /**
     * Maximum length of combined embedding text (chars)
     * Should respect embedding model max tokens
     */
    @Min(200)
    @Max(1000)
    private int combinedKnnMaxLength = 400;
    
    public enum SamplingStrategy {
        TOP_N,      // Just take top N recent jobs
        TF_IDF,     // Extract key terms
        WEIGHTED    // Combination (future)
    }
}
```

**application.yml:**
```yaml
application:
  hr-search:
    # Core search parameters
    max-jobs-for-search: 20
    embedding-snippet-length: 500
    top-qualifications-count: 3
    
    # Combined KNN (Phase 1)
    combined-knn-enabled: true
    combined-knn-sampling-strategy: TOP_N
    combined-knn-sample-size: 5
    combined-knn-max-length: 400
    
    # Future: Phase 2 scoring configs
    # scoring:
    #   absolute-threshold-excellent: 15.0
    #   ...
```

**Tests:**
```java
@SpringBootTest
class HrSearchConfigTest {
    
    @Autowired
    private HrSearchConfig config;
    
    @Test
    void shouldLoadConfigFromYaml() {
        assertThat(config.getMaxJobsForSearch()).isEqualTo(20);
        assertThat(config.isCombinedKnnEnabled()).isTrue();
    }
    
    @Test
    void shouldValidateConfigConstraints() {
        // Test với invalid values
        config.setMaxJobsForSearch(0);  // Should fail @Min(1)
        // ...
    }
}
```

---

#### Task 1.2: Implement Combined Embedding Service (16 hours)

**Role:** Senior Backend Developer  
**Files to create:**
- `services/CombinedEmbeddingService.java`
- `services/impl/CombinedEmbeddingServiceImpl.java`

**Implementation:**

```java
package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.Job;
import java.util.List;

/**
 * Service for creating combined embeddings from multiple jobs
 * Phase 1: Reduce N KNN queries → 1 KNN query
 */
public interface CombinedEmbeddingService {
    
    /**
     * Build combined search text from list of jobs
     * 
     * Strategy:
     * 1. Sample top N jobs (configured)
     * 2. Extract compact search text from each
     * 3. Combine into single text (max configured length)
     * 
     * @param jobs List of company active jobs
     * @return Combined search text for embedding (~400 chars)
     */
    String buildCombinedSearchText(List<Job> jobs);
    
    /**
     * Get combined embedding vector for company jobs
     * 
     * @param jobs List of company active jobs
     * @return Embedding vector (float[] 3072 dims)
     */
    float[] getCombinedEmbedding(List<Job> jobs);
}
```

```java
package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.config.HrSearchConfig;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.services.CombinedEmbeddingService;
import com.hcmute.careergraph.services.EmbedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CombinedEmbeddingServiceImpl implements CombinedEmbeddingService {
    
    private final HrSearchConfig config;
    private final EmbedService embedService;
    
    @Override
    public String buildCombinedSearchText(List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return "";
        }
        
        // Step 1: Sample top N jobs by recency
        int sampleSize = Math.min(config.getCombinedKnnSampleSize(), jobs.size());
        List<Job> sampledJobs = jobs.stream()
            .sorted(Comparator.comparing(Job::getCreatedDate).reversed())
            .limit(sampleSize)
            .collect(Collectors.toList());
        
        log.debug("Sampled {} jobs from {} total for combined embedding", 
                  sampledJobs.size(), jobs.size());
        
        // Step 2: Extract compact search text from each job
        List<String> jobTexts = sampledJobs.stream()
            .map(this::extractCompactJobText)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        
        // Step 3: Combine with length limit
        String combined = String.join(" ", jobTexts);
        
        if (combined.length() > config.getCombinedKnnMaxLength()) {
            combined = combined.substring(0, config.getCombinedKnnMaxLength());
            log.debug("Truncated combined text to {} chars", config.getCombinedKnnMaxLength());
        }
        
        log.debug("Built combined search text: {} chars from {} jobs", 
                  combined.length(), sampledJobs.size());
        
        return combined.trim();
    }
    
    @Override
    public float[] getCombinedEmbedding(List<Job> jobs) {
        String combinedText = buildCombinedSearchText(jobs);
        
        if (!StringUtils.hasText(combinedText)) {
            log.warn("Empty combined text, returning zero vector");
            return new float[3072];  // Zero vector
        }
        
        return embedService.embed(combinedText);
    }
    
    /**
     * Extract compact text from single job
     * Format: title + top qualifications (similar to V2 buildJobSearchText)
     */
    private String extractCompactJobText(Job job) {
        StringBuilder sb = new StringBuilder();
        
        // Title (highest signal)
        if (StringUtils.hasText(job.getTitle())) {
            sb.append(job.getTitle()).append(" ");
        }
        
        // Top qualifications only (not full description)
        if (job.getMinimumQualifications() != null) {
            String quals = job.getMinimumQualifications().stream()
                .limit(2)  // Top 2
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
            sb.append(quals);
        }
        
        return sb.toString().trim();
    }
}
```

**Tests:**
```java
@SpringBootTest
class CombinedEmbeddingServiceTest {
    
    @Autowired
    private CombinedEmbeddingService service;
    
    @Test
    void shouldCombineMultipleJobs() {
        List<Job> jobs = List.of(
            createJob("Frontend Developer", List.of("React", "TypeScript")),
            createJob("Backend Developer", List.of("Java", "Spring Boot")),
            createJob("DevOps Engineer", List.of("AWS", "Kubernetes"))
        );
        
        String combined = service.buildCombinedSearchText(jobs);
        
        assertThat(combined).contains("Frontend", "Backend", "DevOps");
        assertThat(combined.length()).isLessThanOrEqualTo(400);
    }
    
    @Test
    void shouldSampleTopNJobs() {
        // Create 10 jobs, only top 5 should be sampled
        List<Job> jobs = createJobs(10);
        
        String combined = service.buildCombinedSearchText(jobs);
        
        // Verify only recent jobs included
        // ...
    }
}
```

---

#### Task 1.3: Refactor CandidateESServiceImpl to Use Config (12 hours)

**Role:** Senior Backend Developer  
**Files to modify:**
- `services/impl/CandidateESServiceImpl.java`
- `persistence/documents/CandidateES.java`

**Changes in CandidateESServiceImpl.java:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateESServiceImpl implements CandidateESService {
    
    private final HrSearchConfig config;  // NEW: Inject config
    private final CombinedEmbeddingService combinedEmbeddingService;  // NEW
    
    // ... other dependencies
    
    /**
     * V3 Phase 1: Use combined KNN if enabled
     */
    @Override
    public SearchResponse<CandidateES> searchCandidatesForCompany(
        String companyId,
        CandidateFilterRequest filter,
        Pageable pageable) {
        try {
            // Get active jobs (now uses config)
            int maxJobs = config.getMaxJobsForSearch();  // V3: from config
            List<Job> companyJobs = jobRepository
                .findActiveJobsByCompanyId(
                    companyId, 
                    LocalDate.now().toString(), 
                    PageRequest.of(0, maxJobs))
                .getContent();
            
            if (companyJobs.isEmpty()) {
                return searchWithOnlyOpenToWorkFilter(pageable, filter);
            }
            
            // V3 Phase 1: Combined KNN strategy
            if (config.isCombinedKnnEnabled()) {
                return hybridSearchWithCombinedKnn(companyJobs, filter, pageable);
            } else {
                // Fallback: V2 behavior (N separate queries)
                List<String> jobSearchTexts = companyJobs.stream()
                    .map(this::buildJobSearchText)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
                return hybridSearchCandidatesForCompany(jobSearchTexts, filter, pageable);
            }
            
        } catch (Exception e) {
            log.error("Error searching candidates for company {}: {}", companyId, e.getMessage());
            throw new RuntimeException("Failed to search candidates for company", e);
        }
    }
    
    /**
     * V3 Phase 1: NEW method using combined embedding
     */
    private SearchResponse<CandidateES> hybridSearchWithCombinedKnn(
        List<Job> jobs,
        CandidateFilterRequest filter,
        Pageable pageable) {
        try {
            // Build combined search text và embedding
            float[] combinedEmbedding = combinedEmbeddingService.getCombinedEmbedding(jobs);
            String combinedText = combinedEmbeddingService.buildCombinedSearchText(jobs);
            
            return client.search(s -> s
                .index("candidates_es")
                .from((int) pageable.getOffset())
                .size(pageable.getPageSize())
                .query(q -> q.bool(b -> {
                    
                    /* ===== 1. Single KNN Query ===== */
                    b.should(sh -> sh
                        .knn(knn -> knn
                            .field("embedding")
                            .queryVector(toFloatList(combinedEmbedding))
                            .numCandidates(100)
                            .boost(0.7f)));
                    
                    /* ===== 2. BM25 với combined text ===== */
                    b.should(sh -> sh
                        .multiMatch(mm -> mm
                            .query(combinedText)
                            .fields(
                                "desiredPosition^10",
                                "currentJobTitle^7",
                                "skills^6",
                                "cvKeywords^5",
                                "summary^3",
                                "resumeText^1")
                            .type(TextQueryType.BestFields)
                            .operator(Operator.Or)
                            .fuzziness("AUTO")
                            .boost(1.0f)));
                    
                    /* ===== 3. Filter ===== */
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
            log.error("Error in hybrid search with combined KNN: {}", e.getMessage());
            throw new RuntimeException("Failed to execute hybrid search with combined KNN", e);
        }
    }
    
    /**
     * Use config for snippet length
     */
    private String buildJobSearchText(Job job) {
        // ... existing logic but use:
        int topCount = config.getTopQualificationsCount();  // V3: from config
        appendTopLines(sb, job.getMinimumQualifications(), topCount);
        appendTopLines(sb, job.getQualifications(), topCount);
        // ...
    }
}
```

**Changes in CandidateES.java:**

```java
@Document(indexName = "candidates_es")
public class CandidateES {
    
    @Autowired  // Inject config for buildSearchText()
    private HrSearchConfig config;
    
    public String buildSearchText() {
        StringBuilder sb = new StringBuilder();
        
        // ... existing logic
        
        // Use config for snippet length
        if (resumeText != null && !resumeText.isBlank()) {
            int snippetLength = config != null 
                ? config.getEmbeddingSnippetLength() 
                : 500;  // Fallback
            String snippet = resumeText.length() > snippetLength 
                ? resumeText.substring(0, snippetLength) 
                : resumeText;
            sb.append(snippet);
        }
        
        return sb.toString().trim();
    }
}
```

**Tests:**
```java
@SpringBootTest
class CandidateESServiceImplPhase1Test {
    
    @Autowired
    private CandidateESService service;
    
    @MockBean
    private CombinedEmbeddingService combinedEmbeddingService;
    
    @Test
    void shouldUseCombinedKnnWhenEnabled() {
        // Given
        when(config.isCombinedKnnEnabled()).thenReturn(true);
        when(combinedEmbeddingService.getCombinedEmbedding(any()))
            .thenReturn(new float[3072]);
        
        // When
        service.searchCandidatesForCompany("company-123", null, PageRequest.of(0, 10));
        
        // Then
        verify(combinedEmbeddingService, times(1)).getCombinedEmbedding(any());
        // Should NOT call embedService.embed() N times
    }
    
    @Test
    void shouldFallbackToV2WhenCombinedKnnDisabled() {
        // Given
        when(config.isCombinedKnnEnabled()).thenReturn(false);
        
        // When
        service.searchCandidatesForCompany("company-123", null, PageRequest.of(0, 10));
        
        // Then
        verify(combinedEmbeddingService, never()).getCombinedEmbedding(any());
        // Should use V2 logic
    }
}
```

---

#### Task 1.4: Add Configuration Monitoring & Metrics (4 hours)

**Role:** DevOps Engineer  
**Files to create:**
- `config/actuator/HrSearchConfigEndpoint.java`

```java
package com.hcmute.careergraph.config.actuator;

import com.hcmute.careergraph.config.HrSearchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Actuator endpoint to expose current HR Search configuration
 * Access: GET /actuator/hr-search-config
 */
@Component
@Endpoint(id = "hr-search-config")
@RequiredArgsConstructor
public class HrSearchConfigEndpoint {
    
    private final HrSearchConfig config;
    
    @ReadOperation
    public Map<String, Object> getConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("maxJobsForSearch", config.getMaxJobsForSearch());
        configMap.put("embeddingSnippetLength", config.getEmbeddingSnippetLength());
        configMap.put("topQualificationsCount", config.getTopQualificationsCount());
        configMap.put("combinedKnnEnabled", config.isCombinedKnnEnabled());
        configMap.put("combinedKnnSamplingStrategy", config.getCombinedKnnSamplingStrategy());
        configMap.put("combinedKnnSampleSize", config.getCombinedKnnSampleSize());
        configMap.put("combinedKnnMaxLength", config.getCombinedKnnMaxLength());
        return configMap;
    }
}
```

**Prometheus Metrics:**
```java
@Component
@RequiredArgsConstructor
public class HrSearchMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        // Track combined KNN usage
        Gauge.builder("hr_search.combined_knn.enabled", config, 
                      c -> c.isCombinedKnnEnabled() ? 1 : 0)
            .register(meterRegistry);
        
        // Track embedding API calls
        meterRegistry.counter("hr_search.embedding.calls");
        
        // Track search latency by strategy
        Timer.builder("hr_search.latency")
            .tag("strategy", config.isCombinedKnnEnabled() ? "combined" : "separate")
            .register(meterRegistry);
    }
}
```

---

### 3.2 Testing Tasks (QA Engineer)

#### Task 1.5: Performance Benchmarking (16 hours)

**Tool:** JMeter / Artillery / Gatling

**Test Scenarios:**

**Scenario 1: Baseline (V2 behavior)**
```yaml
# artillery-v2-baseline.yml
config:
  target: "http://localhost:8010"
  phases:
    - duration: 300
      arrivalRate: 10
      name: "Warm-up"
    - duration: 600
      arrivalRate: 50
      name: "Load test"
scenarios:
  - name: "No keyword search (V2)"
    flow:
      - post:
          url: "/careergraph/api/v1/candidates/suggestion/search"
          headers:
            Authorization: "Bearer {{token}}"
          json:
            page: 0
            size: 10
```

**Expected V2 Results:**
- P50 latency: ~800ms
- P95 latency: ~1500ms
- P99 latency: ~2000ms
- ES query count: 40 (20 KNN + 20 BM25)

**Scenario 2: V3 Phase 1 (Combined KNN)**
```yaml
# Same test but với combined_knn_enabled=true
```

**Expected V3 Results:**
- P50 latency: ~300ms (62% improvement)
- P95 latency: ~500ms (67% improvement)
- P99 latency: ~800ms (60% improvement)
- ES query count: 2 (1 KNN + 1 BM25)

**Verification:**
```bash
# Compare results
artillery report v2-results.json --output v2-report.html
artillery report v3-results.json --output v3-report.html

# Check metrics
curl http://localhost:8010/actuator/metrics/hr_search.latency
```

---

#### Task 1.6: Regression Testing (12 hours)

**Test Cases:**

| Test ID | Description | V2 Behavior | V3 Expected |
|---------|-------------|-------------|-------------|
| REG-001 | Search với keyword | Works | Same results |
| REG-002 | Search không keyword, company 5 jobs | Works | Same results, faster |
| REG-003 | Search không keyword, company 20 jobs | Works | Same results, much faster |
| REG-004 | Filters: location, experience | Works | Same results |
| REG-005 | Pagination works | Works | Same results |
| REG-006 | Score normalization | Works | Same results |

**Automated Test Suite:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class Phase1RegressionTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturnSameResultsAsV2() throws Exception {
        // Compare V2 vs V3 results
        // ...
    }
}
```

---

#### Task 1.7: Load Testing (12 hours)

**Scenarios:**

1. **Normal Load:** 50 req/s, 10 minutes
2. **Peak Load:** 100 req/s, 5 minutes
3. **Stress Test:** 200 req/s until failure

**Success Criteria:**
- Normal load: P95 < 500ms, 0% errors
- Peak load: P95 < 1000ms, < 0.1% errors
- Stress test: Graceful degradation, no crashes

---

### 3.3 DevOps Tasks

#### Task 1.8: Configuration Deployment Strategy (8 hours)

**Role:** DevOps Engineer

**Strategy: ConfigMap (Kubernetes)**

```yaml
# k8s/configmap-hr-search.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hr-search-config
  namespace: careergraph
data:
  application.yml: |
    application:
      hr-search:
        max-jobs-for-search: 20
        combined-knn-enabled: true
        # ... other configs
```

**Deployment:**
```bash
# Update config without pod restart
kubectl apply -f k8s/configmap-hr-search.yaml

# Trigger config reload (if using Spring Cloud Config)
curl -X POST http://localhost:8010/actuator/refresh

# Or rolling restart
kubectl rollout restart deployment/careergraph-api
```

**Rollback Plan:**
```bash
# Revert to previous version
kubectl apply -f k8s/configmap-hr-search-v2.yaml
kubectl rollout restart deployment/careergraph-api
```

---

#### Task 1.9: Monitoring Setup (8 hours)

**Prometheus Alerts:**
```yaml
# prometheus-alerts.yml
groups:
  - name: hr_search
    interval: 30s
    rules:
      - alert: HighSearchLatency
        expr: histogram_quantile(0.95, hr_search_latency_bucket) > 1000
        for: 5m
        annotations:
          summary: "HR search latency P95 > 1s"
          
      - alert: CombinedKnnErrors
        expr: rate(hr_search_combined_knn_errors_total[5m]) > 0.01
        annotations:
          summary: "Combined KNN error rate > 1%"
```

**Grafana Dashboard:**
```json
{
  "dashboard": {
    "title": "HR Search V3 Phase 1",
    "panels": [
      {
        "title": "Search Latency (P50, P95, P99)",
        "targets": [
          "histogram_quantile(0.50, hr_search_latency_bucket)",
          "histogram_quantile(0.95, hr_search_latency_bucket)",
          "histogram_quantile(0.99, hr_search_latency_bucket)"
        ]
      },
      {
        "title": "Combined KNN Usage",
        "targets": [
          "hr_search_combined_knn_enabled"
        ]
      },
      {
        "title": "Embedding API Calls",
        "targets": [
          "rate(hr_search_embedding_calls_total[5m])"
        ]
      }
    ]
  }
}
```

---

## 4. Deployment Plan

### 4.1 Week 1-2: Development

**Day 1-3:** Task 1.1 (Config infrastructure)  
**Day 4-7:** Task 1.2 (Combined embedding service)  
**Day 8-10:** Task 1.3 (Refactor CandidateESServiceImpl)

### 4.2 Week 3: Testing & Staging

**Day 1-2:** Task 1.5 (Performance benchmarking)  
**Day 3-4:** Task 1.6 (Regression testing)  
**Day 5:** Task 1.7 (Load testing)

### 4.3 Week 4: Production Deployment

**Day 1:** Deploy to staging with `combined_knn_enabled=false`  
**Day 2:** Enable combined KNN in staging, monitor 24h  
**Day 3:** Canary deployment (10% traffic)  
**Day 4:** Ramp to 50% traffic  
**Day 5:** Full rollout (100%)

---

## 5. Success Criteria

### 5.1 Performance Metrics

- [ ] P95 latency < 500ms (vs 1500ms V2)
- [ ] P99 latency < 1000ms (vs 2000ms V2)
- [ ] ES query count: 2 (vs 40 V2)
- [ ] Embedding API calls: 1/search (vs 20 V2)

### 5.2 Quality Metrics

- [ ] Search results precision: No regression vs V2
- [ ] Search results recall: No regression vs V2
- [ ] Zero errors during load test

### 5.3 Operational Metrics

- [ ] Config reload time < 1 minute
- [ ] Rollback time < 5 minutes
- [ ] Monitoring dashboard operational

---

## 6. Rollback Triggers

| Trigger | Threshold | Action |
|---------|-----------|--------|
| Search latency | P95 > 1500ms (V2 baseline) | Immediate rollback |
| Error rate | > 0.5% | Immediate rollback |
| Search quality | Precision drop > 5% | Investigate, rollback if confirmed |
| Elasticsearch CPU | > 80% sustained | Rollback, investigate |

---

## 7. Post-Implementation Report Template

**File:** `PHASE1_IMPLEMENTATION_REPORT.md`

**Sections:**
1. Executive Summary
2. Implementation Details
   - Config infrastructure
   - Combined embedding service
   - Refactoring changes
3. Testing Results
   - Performance benchmarks (before/after charts)
   - Regression test results
   - Load test results
4. Deployment Log
   - Timeline
   - Issues encountered
   - Resolutions
5. Metrics & KPIs
   - Latency improvements
   - Cost savings (ES queries, embedding API)
6. Lessons Learned
7. Recommendations for Phase 2

---

## 8. Dependencies

### 8.1 External Dependencies

- Spring Boot 3.4.6
- Elasticsearch 8.x
- Prometheus + Grafana
- Kubernetes (if using ConfigMap strategy)

### 8.2 Internal Dependencies

- V2 implementation completed
- Embedding service operational
- Database migrations (none needed for Phase 1)

---

## 9. Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Combined embedding less accurate | HIGH | A/B test before full rollout |
| Config hot reload fails | MEDIUM | Document manual restart procedure |
| Performance regression | HIGH | Benchmark before deployment, rollback plan ready |
| Elasticsearch cluster overload | HIGH | Monitor closely, scale if needed |

---

**Sign-off:**

- [ ] Tech Lead approved
- [ ] QA signed off
- [ ] DevOps ready
- [ ] Phase 1 deployment authorized

**Next:** Proceed to Phase 2 (Hybrid Scoring System)
