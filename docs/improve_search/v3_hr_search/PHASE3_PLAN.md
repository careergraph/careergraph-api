# Phase 3: CV Keywords Quality Validation

**Phase:** 3 of 3  
**Duration:** 2 weeks (60 hours Senior Dev + 40 hours ML Engineer + 40 hours QA)  
**Priority:** 🟡 MEDIUM  
**Status:** 📋 PLANNING  
**Prerequisites:** ✅ Phase 1 completed (for config infrastructure)  

---

## 1. Context & Business Case

### 1.1 Current Problem (Gemini Hallucination Risk)

**Scenario từ Khách hàng khó tính:**

> "Candidate upload CV: 'Java Backend Developer, 5 years Spring Boot, MySQL, REST API'  
> Gemini extracts cvKeywords: 'Python, Django, PostgreSQL, Machine Learning'  
> (Completely hallucinated!)
>   
> HR search 'Python' → This candidate shows up with HIGH score (cvKeywords^5)  
> Reality: Candidate has ZERO Python experience."

**Root Cause:**  
- Gemini temperature = 0.4 → still allows some creativity
- Prompt không đủ strict
- No validation after extraction

**Impact:**
- **Search precision drops:** Irrelevant candidates ranked high
- **False positives:** HR wastes time on wrong candidates
- **User trust issues:** "Tại sao search không accurate?"

### 1.2 Business Goals

1. **Ensure Quality:** 95% cvKeywords có similarity > 0.6 vs resumeText
2. **Auto-fallback:** Automatically use resumeText nếu keywords quality poor
3. **Monitoring:** Real-time dashboard cho extraction quality

### 1.3 Proposed Solution: Quality Validation Pipeline

```
┌─────────────────────────────────────────────────────────┐
│         CV Keywords Quality Validation Pipeline          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  File.cvKeywordsJson (from Gemini)                       │
│         ↓                                                │
│  CvKeywordsQualityService.validate()                     │
│  ├─ Step 1: Cosine Similarity Check                     │
│  │   embed(cvKeywords) vs embed(resumeText)             │
│  │   → similarity score (0-1)                           │
│  ├─ Step 2: Keyword Density Analysis                    │
│  │   Count keywords in resumeText                        │
│  │   → density score (0-1)                              │
│  ├─ Step 3: Overall Quality Score                       │
│  │   weighted average → (0-100)                         │
│  └─ Step 4: Pass/Fail Decision                          │
│      if quality < threshold: fallback to resumeText     │
│         ↓                                                │
│  CandidateES.cvKeywords (validated)                     │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Technical Design

### 2.1 Quality Validation Architecture

```
Component Diagram:

┌───────────────────────────────────────────────────────────┐
│                  CvKeywordsQualityService                  │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  validate(File file): CvKeywordsQuality {                 │
│    1. Extract cvKeywords from file.cvKeywordsJson         │
│    2. Extract resumeText from file.resumeExtractedText    │
│    3. Compute similarity score (cosine)                   │
│    4. Compute density score (keyword presence)            │
│    5. Compute overall quality                             │
│    6. Determine pass/fail                                 │
│    return CvKeywordsQuality { ... }                       │
│  }                                                         │
│                                                            │
│  Dependencies:                                             │
│  - EmbedService (for cosine similarity)                   │
│  - NLP library (for keyword extraction & counting)        │
│  - HrSearchConfig (for thresholds)                        │
└───────────────────────────────────────────────────────────┘
```

### 2.2 Configuration (extends Phase 1 config)

```yaml
application:
  hr-search:
    quality-validation:
      # Enable/disable validation
      enabled: true
      
      # Similarity threshold (cosine similarity)
      min-similarity-threshold: 0.6  # 60% similarity required
      
      # Density threshold (keyword presence in resumeText)
      min-density-threshold: 0.3     # 30% keywords must appear in CV
      
      # Overall quality weights
      weight-similarity: 0.7          # 70% weight on similarity
      weight-density: 0.3             # 30% weight on density
      
      # Auto-fallback
      auto-fallback-enabled: true     # Auto use resumeText if quality low
      fallback-quality-threshold: 60  # Quality < 60 → fallback
      
      # Monitoring
      alert-on-low-quality-rate: 0.1  # Alert if > 10% fail validation
```

---

## 3. Implementation Tasks

### 3.1 Backend Implementation (Senior Backend Developer)

#### Task 3.1: Create Quality Validation Models (4 hours)

**Files to create:**
- `persistence/dtos/response/CvKeywordsQuality.java`
- `persistence/enums/QualityStatus.java`

```java
package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.persistence.enums.QualityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Quality validation result for CV keywords extraction
 * Phase 3: Ensure Gemini extraction accuracy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvKeywordsQuality {
    
    /**
     * Cosine similarity between cvKeywords embedding and resumeText embedding
     * Range: 0.0 - 1.0
     * Higher = more similar (better quality)
     */
    private float similarityScore;
    
    /**
     * Keyword density: % of keywords that actually appear in resumeText
     * Range: 0.0 - 1.0
     * Higher = more keywords found in CV (better quality)
     */
    private float densityScore;
    
    /**
     * Overall quality score (weighted average)
     * Range: 0 - 100
     * Formula: (similarityScore × 0.7 + densityScore × 0.3) × 100
     */
    private float overallQuality;
    
    /**
     * Does this pass validation?
     * true: quality >= threshold, can use cvKeywords
     * false: quality < threshold, should fallback to resumeText
     */
    private boolean passValidation;
    
    /**
     * Validation status
     */
    private QualityStatus status;
    
    /**
     * If validation failed, why?
     * E.g., "Similarity too low (0.45 < 0.6)"
     */
    private String fallbackReason;
    
    /**
     * When was this validated
     */
    private LocalDateTime validatedAt;
    
    /**
     * File ID that was validated
     */
    private String fileId;
}
```

```java
package com.hcmute.careergraph.persistence.enums;

public enum QualityStatus {
    EXCELLENT,   // Quality >= 80
    GOOD,        // Quality >= 70
    ACCEPTABLE,  // Quality >= 60
    POOR,        // Quality < 60
    FAILED       // Validation error
}
```

**Update File.java:**
```java
@Entity
public class File {
    // ... existing fields
    
    /**
     * V3 Phase 3: Quality validation result (stored as JSON)
     */
    @Column(name = "cv_keywords_quality_json", columnDefinition = "TEXT")
    private String cvKeywordsQualityJson;
    
    /**
     * V3 Phase 3: Transient field for quality object
     */
    @Transient
    private CvKeywordsQuality cvKeywordsQuality;
}
```

---

#### Task 3.2: Implement CvKeywordsQualityService (20 hours)

**Service Interface:**
```java
package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.CvKeywordsQuality;
import com.hcmute.careergraph.persistence.models.File;

/**
 * Service for validating CV keywords extraction quality
 * Phase 3: Ensure Gemini doesn't hallucinate
 */
public interface CvKeywordsQualityService {
    
    /**
     * Validate cvKeywords quality for a file
     * 
     * @param file File entity with cvKeywordsJson and resumeExtractedText
     * @return Quality validation result
     */
    CvKeywordsQuality validate(File file);
    
    /**
     * Batch validate multiple files (async)
     * 
     * @param files List of files to validate
     * @return List of quality results
     */
    List<CvKeywordsQuality> validateBatch(List<File> files);
    
    /**
     * Get quality statistics for monitoring
     * 
     * @param startDate Start date for stats
     * @param endDate End date for stats
     * @return Quality statistics
     */
    QualityStatistics getStatistics(LocalDate startDate, LocalDate endDate);
}
```

**Implementation:**
```java
package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.config.HrSearchConfig;
import com.hcmute.careergraph.persistence.dtos.response.CvKeywordsQuality;
import com.hcmute.careergraph.persistence.enums.QualityStatus;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.services.CvKeywordsQualityService;
import com.hcmute.careergraph.services.EmbedService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvKeywordsQualityServiceImpl implements CvKeywordsQualityService {
    
    private final EmbedService embedService;
    private final HrSearchConfig config;
    private final ObjectMapper objectMapper;
    
    @Override
    public CvKeywordsQuality validate(File file) {
        if (file == null) {
            return createFailedResult("File is null");
        }
        
        // Extract cvKeywords from JSON
        String cvKeywords = extractCvKeywords(file.getCvKeywordsJson());
        String resumeText = file.getResumeExtractedText();
        
        // Validation checks
        if (!StringUtils.hasText(cvKeywords)) {
            return createFailedResult("CV keywords not available");
        }
        
        if (!StringUtils.hasText(resumeText)) {
            return createFailedResult("Resume text not available");
        }
        
        try {
            // Step 1: Compute similarity score
            float similarityScore = computeSimilarityScore(cvKeywords, resumeText);
            
            // Step 2: Compute density score
            float densityScore = computeDensityScore(cvKeywords, resumeText);
            
            // Step 3: Overall quality (weighted average)
            float overallQuality = computeOverallQuality(similarityScore, densityScore);
            
            // Step 4: Pass/fail decision
            boolean passValidation = overallQuality >= config.getQualityValidation().getFallbackQualityThreshold();
            
            QualityStatus status = determineStatus(overallQuality);
            
            String fallbackReason = null;
            if (!passValidation) {
                fallbackReason = buildFallbackReason(similarityScore, densityScore, overallQuality);
            }
            
            log.info("CV keywords quality validation for file {}: similarity={}, density={}, overall={}, pass={}",
                     file.getId(), similarityScore, densityScore, overallQuality, passValidation);
            
            return CvKeywordsQuality.builder()
                .similarityScore(similarityScore)
                .densityScore(densityScore)
                .overallQuality(overallQuality)
                .passValidation(passValidation)
                .status(status)
                .fallbackReason(fallbackReason)
                .validatedAt(LocalDateTime.now())
                .fileId(file.getId())
                .build();
                
        } catch (Exception e) {
            log.error("Error validating CV keywords quality for file {}: {}", 
                      file.getId(), e.getMessage(), e);
            return createFailedResult("Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Compute cosine similarity between cvKeywords and resumeText
     * Uses embedding vectors for semantic similarity
     */
    private float computeSimilarityScore(String cvKeywords, String resumeText) {
        // Get embeddings
        float[] keywordsEmbedding = embedService.embed(cvKeywords);
        
        // Sample resumeText if too long (to match embedding model limits)
        String resumeSample = resumeText.length() > 1000 
            ? resumeText.substring(0, 1000) 
            : resumeText;
        float[] resumeEmbedding = embedService.embed(resumeSample);
        
        // Compute cosine similarity
        return cosineSimilarity(keywordsEmbedding, resumeEmbedding);
    }
    
    /**
     * Compute keyword density: % of keywords found in resumeText
     */
    private float computeDensityScore(String cvKeywords, String resumeText) {
        // Extract individual keywords (split by space, comma, etc.)
        Set<String> keywords = tokenize(cvKeywords);
        
        if (keywords.isEmpty()) {
            return 0f;
        }
        
        // Normalize resumeText for matching
        String normalizedResume = resumeText.toLowerCase();
        
        // Count how many keywords appear in resume
        long foundCount = keywords.stream()
            .filter(keyword -> normalizedResume.contains(keyword.toLowerCase()))
            .count();
        
        return (float) foundCount / keywords.size();
    }
    
    /**
     * Compute overall quality score (weighted average)
     */
    private float computeOverallQuality(float similarityScore, float densityScore) {
        float weightSimilarity = config.getQualityValidation().getWeightSimilarity();
        float weightDensity = config.getQualityValidation().getWeightDensity();
        
        // Convert to 0-100 scale
        return (similarityScore * weightSimilarity + densityScore * weightDensity) * 100f;
    }
    
    /**
     * Cosine similarity between two vectors
     */
    private float cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA.length != vecB.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;
        
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        
        if (normA == 0 || normB == 0) {
            return 0f;
        }
        
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Tokenize keywords string into set of individual keywords
     */
    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("[,;\\s]+"))
            .map(String::trim)
            .filter(s -> s.length() > 2)  // Filter out very short tokens
            .collect(Collectors.toSet());
    }
    
    /**
     * Determine quality status based on overall score
     */
    private QualityStatus determineStatus(float overallQuality) {
        if (overallQuality >= 80) return QualityStatus.EXCELLENT;
        if (overallQuality >= 70) return QualityStatus.GOOD;
        if (overallQuality >= 60) return QualityStatus.ACCEPTABLE;
        return QualityStatus.POOR;
    }
    
    /**
     * Build human-readable fallback reason
     */
    private String buildFallbackReason(float similarity, float density, float overall) {
        StringBuilder sb = new StringBuilder();
        
        float minSimilarity = config.getQualityValidation().getMinSimilarityThreshold();
        float minDensity = config.getQualityValidation().getMinDensityThreshold();
        
        if (similarity < minSimilarity) {
            sb.append(String.format("Similarity too low (%.2f < %.2f). ", similarity, minSimilarity));
        }
        
        if (density < minDensity) {
            sb.append(String.format("Keyword density too low (%.2f < %.2f). ", density, minDensity));
        }
        
        sb.append(String.format("Overall quality: %.0f < 60.", overall));
        
        return sb.toString();
    }
    
    /**
     * Create failed validation result
     */
    private CvKeywordsQuality createFailedResult(String reason) {
        return CvKeywordsQuality.builder()
            .similarityScore(0f)
            .densityScore(0f)
            .overallQuality(0f)
            .passValidation(false)
            .status(QualityStatus.FAILED)
            .fallbackReason(reason)
            .validatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Extract searchKeywords from cvKeywordsJson
     */
    private String extractCvKeywords(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode searchKeywords = node.get("searchKeywords");
            if (searchKeywords != null && !searchKeywords.isNull()) {
                return searchKeywords.asText("");
            }
        } catch (Exception e) {
            log.warn("Failed to parse cvKeywordsJson: {}", e.getMessage());
        }
        
        return null;
    }
}
```

---

#### Task 3.3: Integrate Validation into ES Indexing (12 hours)

**Update CandidateESServiceImpl.java:**

```java
@Service
public class CandidateESServiceImpl implements CandidateESService {
    
    private final CvKeywordsQualityService qualityService;
    
    /**
     * V3 Phase 3: Validate cvKeywords quality before indexing
     */
    private ResumeProjection resolveResume(String candidateId) {
        return fileRepository
            .findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(...)
            .filter(file -> StringUtils.hasText(file.getResumeExtractedText()))
            .map(file -> {
                String cvKeywords = null;
                
                // V3 Phase 3: Quality validation
                if (config.getQualityValidation().isEnabled()) {
                    cvKeywords = resolveValidatedCvKeywords(file);
                } else {
                    // No validation: use keywords as-is
                    cvKeywords = extractCvKeywords(file);
                }
                
                return new ResumeProjection(
                    file.getResumeExtractedText(),
                    cvKeywords,
                    file.getId(),
                    file.getLastModifiedDate(),
                    file.getResumeContentHash());
            })
            .orElse(ResumeProjection.empty());
    }
    
    /**
     * V3 Phase 3: Resolve cvKeywords with quality validation
     */
    private String resolveValidatedCvKeywords(File file) {
        // Check if already validated (cached in DB)
        if (StringUtils.hasText(file.getCvKeywordsQualityJson())) {
            try {
                CvKeywordsQuality quality = objectMapper.readValue(
                    file.getCvKeywordsQualityJson(), 
                    CvKeywordsQuality.class);
                
                if (quality.isPassValidation()) {
                    // Quality good: use cvKeywords
                    return extractCvKeywords(file);
                } else {
                    // Quality poor: fallback to resumeText snippet
                    log.warn("CV keywords quality validation failed for file {}: {}. Using fallback.", 
                             file.getId(), quality.getFallbackReason());
                    return null;  // Will trigger resumeText fallback in buildSearchText()
                }
            } catch (Exception e) {
                log.warn("Failed to parse cvKeywordsQualityJson: {}", e.getMessage());
            }
        }
        
        // Not validated yet: validate now
        CvKeywordsQuality quality = qualityService.validate(file);
        
        // Persist quality result to DB (async)
        persistQualityResult(file.getId(), quality);
        
        if (quality.isPassValidation()) {
            return extractCvKeywords(file);
        } else {
            log.warn("CV keywords validation failed: {}. Using fallback.", quality.getFallbackReason());
            return null;
        }
    }
    
    /**
     * Persist quality validation result to database (async)
     */
    @Async
    private void persistQualityResult(String fileId, CvKeywordsQuality quality) {
        try {
            String json = objectMapper.writeValueAsString(quality);
            fileRepository.updateCvKeywordsQualityJson(fileId, json);
        } catch (Exception e) {
            log.error("Failed to persist quality result for file {}: {}", fileId, e.getMessage());
        }
    }
}
```

---

#### Task 3.4: Add Monitoring & Metrics (8 hours)

**Prometheus Metrics:**
```java
@Component
@RequiredArgsConstructor
public class CvKeywordsQualityMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        // Quality distribution
        Gauge.builder("cv_keywords.quality.similarity", () -> currentAverageSimilarity())
            .register(meterRegistry);
        
        Gauge.builder("cv_keywords.quality.density", () -> currentAverageDensity())
            .register(meterRegistry);
        
        // Validation pass rate
        meterRegistry.counter("cv_keywords.validation.passed");
        meterRegistry.counter("cv_keywords.validation.failed");
        
        // Fallback trigger rate
        meterRegistry.counter("cv_keywords.fallback.triggered");
    }
    
    public void recordValidationResult(CvKeywordsQuality quality) {
        if (quality.isPassValidation()) {
            meterRegistry.counter("cv_keywords.validation.passed").increment();
        } else {
            meterRegistry.counter("cv_keywords.validation.failed").increment();
            meterRegistry.counter("cv_keywords.fallback.triggered").increment();
        }
    }
}
```

**Grafana Dashboard:**
```json
{
  "dashboard": {
    "title": "CV Keywords Quality (Phase 3)",
    "panels": [
      {
        "title": "Validation Pass Rate",
        "targets": [
          "rate(cv_keywords_validation_passed_total[5m]) / (rate(cv_keywords_validation_passed_total[5m]) + rate(cv_keywords_validation_failed_total[5m]))"
        ],
        "threshold": 0.95  // Alert if < 95%
      },
      {
        "title": "Average Similarity Score",
        "targets": ["cv_keywords_quality_similarity"]
      },
      {
        "title": "Average Density Score",
        "targets": ["cv_keywords_quality_density"]
      },
      {
        "title": "Fallback Trigger Rate",
        "targets": ["rate(cv_keywords_fallback_triggered_total[5m])"]
      }
    ]
  }
}
```

---

### 3.2 Testing Tasks (QA Engineer)

#### Task 3.5: Quality Validation Testing (20 hours)

**Test Cases:**

| Test ID | Setup | Expected Validation Result |
|---------|-------|----------------------------|
| **QV-001** | cvKeywords = "Java Spring Boot MySQL", resumeText contains all keywords | similarity > 0.8, density = 1.0, PASS |
| **QV-002** | cvKeywords = "Python Django", resumeText = "Java Spring Boot" (hallucinated) | similarity < 0.3, density = 0, FAIL → fallback |
| **QV-003** | cvKeywords = "React TypeScript", resumeText mentions "React" only | similarity ~ 0.6, density = 0.5, PASS/BORDERLINE |
| **QV-004** | cvKeywords = very generic terms, resumeText = specific tech stack | density low, FAIL → fallback |
| **QV-005** | resumeText empty or null | Validation skipped, use cvKeywords as-is |

**Automated Tests:**
```java
@SpringBootTest
class CvKeywordsQualityServiceTest {
    
    @Autowired
    private CvKeywordsQualityService service;
    
    @Test
    void shouldPassValidationForAccurateKeywords() {
        File file = createFileWithKeywords(
            "Java Spring Boot MySQL",
            "10 years Java developer, expert in Spring Boot and MySQL databases"
        );
        
        CvKeywordsQuality result = service.validate(file);
        
        assertThat(result.isPassValidation()).isTrue();
        assertThat(result.getSimilarityScore()).isGreaterThan(0.7f);
        assertThat(result.getDensityScore()).isGreaterThan(0.8f);
    }
    
    @Test
    void shouldFailValidationForHallucinatedKeywords() {
        File file = createFileWithKeywords(
            "Python Django TensorFlow",  // Hallucinated
            "Java Spring Boot developer with 5 years experience"  // Reality
        );
        
        CvKeywordsQuality result = service.validate(file);
        
        assertThat(result.isPassValidation()).isFalse();
        assertThat(result.getSimilarityScore()).isLessThan(0.5f);
        assertThat(result.getFallbackReason()).contains("Similarity too low");
    }
}
```

---

#### Task 3.6: End-to-End Testing (12 hours)

**Scenario: Full pipeline with quality validation**

```
1. Candidate uploads CV (Java developer)
2. Text extraction: "Java Spring Boot MySQL REST API"
3. Gemini extraction: 
   - Scenario A (accurate): "Java Spring Boot MySQL"
   - Scenario B (hallucinated): "Python Django PostgreSQL"
4. Quality validation:
   - Scenario A: PASS → use cvKeywords
   - Scenario B: FAIL → fallback to resumeText snippet
5. ES indexing: cvKeywords field populated correctly
6. HR search "Java":
   - Scenario A: High match (cvKeywords^5)
   - Scenario B: Medium match (resumeText^1)
```

**Verification:**
- Check ES index: `cvKeywords` field content
- Check database: `cv_keywords_quality_json` column
- Check metrics: validation pass rate
- Check logs: fallback triggers

---

### 3.3 ML Engineer Tasks

#### Task 3.7: Threshold Optimization (16 hours)

**Goal:** Find optimal thresholds for similarity and density

**Method: Grid Search with 80/20 split**

```python
# Pseudo-code for threshold optimization
import pandas as pd
from sklearn.metrics import precision_score, recall_score

# Load validation dataset
# 100 CVs with manual labels: ACCURATE / HALLUCINATED
df = load_validation_dataset()

# Grid search
similarity_thresholds = [0.5, 0.55, 0.6, 0.65, 0.7]
density_thresholds = [0.2, 0.25, 0.3, 0.35, 0.4]

results = []
for sim_thresh in similarity_thresholds:
    for dens_thresh in density_thresholds:
        # Apply thresholds
        df['predicted'] = df.apply(lambda row: 
            'ACCURATE' if (row['similarity'] >= sim_thresh and 
                           row['density'] >= dens_thresh) 
            else 'HALLUCINATED', axis=1)
        
        # Compute metrics
        precision = precision_score(df['label'], df['predicted'], pos_label='ACCURATE')
        recall = recall_score(df['label'], df['predicted'], pos_label='ACCURATE')
        f1 = 2 * (precision * recall) / (precision + recall)
        
        results.append({
            'sim_threshold': sim_thresh,
            'dens_threshold': dens_thresh,
            'precision': precision,
            'recall': recall,
            'f1': f1
        })

# Select best thresholds (max F1)
best = max(results, key=lambda x: x['f1'])
print(f"Optimal thresholds: similarity={best['sim_threshold']}, density={best['dens_threshold']}")
```

**Expected Outcome:**
- Optimal similarity threshold: ~0.6
- Optimal density threshold: ~0.3
- Validation accuracy: > 90%

---

## 4. Deployment Strategy

### 4.1 Week 1-2: Development + ML Tuning

**Day 1-3:** Tasks 3.1, 3.2 (Models + Service)  
**Day 4-6:** Task 3.3 (ES integration)  
**Day 7-8:** Task 3.4 (Monitoring)  
**Day 9-10:** Task 3.7 (ML threshold optimization)

### 4.2 Week 3: Testing & Staging

**Day 1-3:** Task 3.5 (Quality validation tests)  
**Day 4-5:** Task 3.6 (E2E tests)

### 4.3 Week 4: Production Deployment

**Day 1:** Deploy với `quality-validation.enabled=true` nhưng `auto-fallback-enabled=false` (monitor only)  
**Day 2-3:** Collect metrics, analyze fallback rate  
**Day 4:** Enable auto-fallback if metrics look good  
**Day 5:** Full production rollout

---

## 5. Success Criteria

### 5.1 Quality Metrics

- [ ] **Validation Pass Rate:** > 95% (< 5% trigger fallback)
- [ ] **Similarity Score (avg):** > 0.7
- [ ] **Density Score (avg):** > 0.5
- [ ] **Precision:** > 90% (hallucinations caught)

### 5.2 Search Quality

- [ ] **Search Precision:** +10% vs V2 (measured by HR feedback)
- [ ] **False Positive Rate:** -20% vs V2

### 5.3 Operational

- [ ] **Validation Latency:** < 200ms per file
- [ ] **Zero manual intervention** needed for fallback

---

## 6. Rollback Plan

### Rollback Triggers

- Validation pass rate < 80% (too many false negatives)
- Search quality regression
- Performance degradation (validation adds latency)

### Rollback Steps

1. Set `quality-validation.enabled=false`
2. Restart services (or use config reload)
3. Monitor for 1 hour
4. Post-mortem: analyze why validation failed

---

## 7. Post-Implementation Report Template

**File:** `PHASE3_IMPLEMENTATION_REPORT.md`

**Sections:**
1. Executive Summary
2. Quality Validation Results
   - Pass rate statistics
   - Similarity/density distributions
   - Fallback trigger rate
3. ML Threshold Optimization
   - Grid search results
   - Final thresholds selected
4. Search Quality Improvement
   - Before/after metrics
   - HR feedback
5. Implementation Details
6. Monitoring Dashboard Screenshots
7. Lessons Learned
8. Future Recommendations (V4)

---

**Sign-off:**

- [ ] Tech Lead approved
- [ ] ML Engineer signed off
- [ ] QA validated
- [ ] Phase 3 deployment authorized

**V3 COMPLETE!** 🎉

---

## 8. Future Enhancements (V4 Roadmap)

**Post-V3 ideas:**

1. **Active Learning:**  
   - HR feedback loop: "Was this candidate relevant?"
   - Retrain similarity threshold based on feedback

2. **Multi-language Support:**  
   - Validate keywords in Vietnamese, English, mixed

3. **Keyword Suggestion:**  
   - If cvKeywords quality poor, suggest corrections to candidate

4. **Real-time Validation:**  
   - Validate immediately after Gemini extraction
   - Show quality score to candidate before they finalize

5. **Advanced NLP:**  
   - Named Entity Recognition (NER) for tech stacks
   - Semantic role labeling for job titles

---

**Congratulations! V3 HR Search implementation complete.**
