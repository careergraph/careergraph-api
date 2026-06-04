# AI Agent Prompt Guide — V3 HR Search Implementation

**Purpose:** Hướng dẫn cách prompt AI agent để thực hiện V3 plan một cách hiệu quả  
**Target:** Human developer sử dụng AI agent (GitHub Copilot, Claude, ChatGPT, etc.)  
**Version:** 1.0.0  

---

## 📋 Table of Contents

1. [General Principles](#general-principles)
2. [Phase 1 Prompt Guide](#phase-1-prompt-guide)
3. [Phase 2 Prompt Guide](#phase-2-prompt-guide)
4. [Phase 3 Prompt Guide](#phase-3-prompt-guide)
5. [Verification Checklist](#verification-checklist)
6. [Troubleshooting](#troubleshooting)

---

## 🎯 General Principles

### 1. Context First, Task Second

**❌ BAD Prompt:**
```
Implement Phase 1 of V3 plan
```

**✅ GOOD Prompt:**
```
Đóng vai trò là Senior Backend Developer với 15+ năm kinh nghiệm Spring Boot.

Hãy đọc các file sau để hiểu context:
1. docs/improve_search/v3_hr_search/PHASE1_PLAN.md (Phase 1 detailed plan)
2. docs/improve_search/v2_hr_search/V2_HR_SEARCH_IMPLEMENTATION_REPORT.md (V2 current state)
3. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java (current implementation)

Sau đó thực hiện Phase 1: Configuration Management & Combined KNN theo plan.

Yêu cầu:
- Tuân thủ 100% code examples trong plan
- Đảm bảo backward compatibility với V2
- Add comprehensive comments (Vietnamese OK)
- Write unit tests cho mọi service method
- Update application.yml với all new properties

Khi xong, báo cáo:
1. Các file đã tạo/sửa
2. Breaking changes (nếu có)
3. How to test locally
```

### 2. Break Down Large Tasks

**Don't:** "Implement entire Phase 1"  
**Do:** "Implement Phase 1 Task 1.1 only: Create HrSearchConfig.java"

### 3. Provide Examples from Current Codebase

**Include:**
- Current code snippet AI cần refactor
- Related classes AI cần reference
- Existing patterns AI nên follow

### 4. Specify Output Format

**Example:**
```
Sau khi implement, hãy tạo file PHASE1_PROGRESS.md với format:

## Completed Tasks
- [x] Task 1.1: HrSearchConfig.java
  - File: src/main/.../config/HrSearchConfig.java
  - Lines: 1-150
  - Tests: HrSearchConfigTest.java (5 test cases)

## Current Issues
- [ ] Compilation error in CandidateESServiceImpl line 245

## Next Steps
- [ ] Task 1.2: Refactor CandidateESServiceImpl
```

---

## 🔴 Phase 1 Prompt Guide

### Phase 1.1: Create Configuration Infrastructure

#### Prompt Template

```markdown
# Phase 1 Task 1.1: Create HrSearchConfig.java

## Context
Đóng vai trò là Senior Backend Developer. 
Hiện tại V2 có các hard-coded constants trong CandidateESServiceImpl.java:
- MAX_JOBS_FOR_SEARCH = 20
- SNIPPET_LENGTH = 500
- TOP_QUALIFICATIONS_COUNT = 3

Goal: Externalize thành @ConfigurationProperties để dễ tune không cần rebuild.

## Files to Read First
1. docs/improve_search/v3_hr_search/PHASE1_PLAN.md (section 3.1 Task 1.1)
2. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java (current constants)
3. src/main/resources/application.yml (để add new properties)

## Implementation Requirements
1. Tạo HrSearchConfig.java theo EXACT code example trong PHASE1_PLAN.md (lines 180-350)
2. Tạo application.yml section:
   ```yaml
   application:
     hr-search:
       max-jobs-for-search: 20
       embedding-snippet-length: 500
       top-qualifications-count: 3
       combined-knn-enabled: false  # Feature flag
   ```
3. Tạo HrSearchConfigTest.java với test cases:
   - testDefaultValues()
   - testCustomValues()
   - testValidation() (negative values should throw exception)

## Deliverables
- [ ] HrSearchConfig.java created
- [ ] application.yml updated
- [ ] HrSearchConfigTest.java with 100% coverage
- [ ] README: How to override config values

## Verification Command
```bash
mvn clean test -Dtest=HrSearchConfigTest
```

Expected: All tests PASS, no compilation errors.
```

#### Files to Attach/Reference

**Must Read:**
1. `docs/improve_search/v3_hr_search/PHASE1_PLAN.md`
2. `docs/improve_search/v2_hr_search/V2_HR_SEARCH_IMPLEMENTATION_REPORT.md`

**Current Code to Reference:**
1. `src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java`
2. `src/main/resources/application.yml`

**Example Tests to Follow Pattern:**
1. `src/test/java/com/hcmute/careergraph/services/impl/*Test.java` (any existing test)

---

### Phase 1.2: Implement Combined KNN

#### Prompt Template

```markdown
# Phase 1 Task 1.2: Implement CombinedEmbeddingService

## Context
Đóng vai trò Senior Backend Developer.

**Current Problem (V2):**
Khi HR search với job.id, code hiện tại:
1. Load top 20 jobs from DB
2. For each job: call embedService.embed(buildJobSearchText(job)) → 20 embedding calls
3. For each job: KNN query to ES → 20 ES queries

**Total:** 20 embedding calls + 20 ES queries = SLOW (P95 = 1500ms)

**Target (V3 Phase 1):**
1. Load top 5 jobs from DB (sampling)
2. Combine all job texts into ONE string (max 400 chars)
3. Call embedService.embed(combinedText) → 1 embedding call
4. KNN query to ES with combined vector → 1 ES query

**Total:** 1 embedding call + 1 ES query = FAST (target: P95 < 500ms)

## Files to Read First
1. docs/improve_search/v3_hr_search/PHASE1_PLAN.md (section 3.2 Task 1.2)
2. src/main/java/com/hcmute/careergraph/services/EmbedService.java (current embedding service)
3. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java (method: suggestCandidatesForJobs)

## Implementation Steps
1. Create CombinedEmbeddingService interface + implementation
   - Method: `float[] combineAndEmbed(List<Job> jobs, HrSearchConfig config)`
   - Logic: Sample jobs → build combined text → embed → return vector

2. Refactor CandidateESServiceImpl:
   ```java
   // BEFORE (V2)
   for (Job job : jobs) {
       float[] vector = embedService.embed(buildJobSearchText(job));
       // ... KNN query
   }
   
   // AFTER (V3)
   if (config.isCombinedKnnEnabled()) {
       float[] combinedVector = combinedEmbeddingService.combineAndEmbed(jobs, config);
       // ... ONE KNN query with combinedVector
   } else {
       // V2 fallback (for A/B testing)
   }
   ```

3. Add feature flag logic:
   - If `combined-knn-enabled: true` → use combined approach
   - If `combined-knn-enabled: false` → use V2 approach

4. Write tests:
   - CombinedEmbeddingServiceTest (unit tests)
   - CandidateESServiceIntegrationTest (performance test)

## Deliverables
- [ ] CombinedEmbeddingService.java (interface)
- [ ] CombinedEmbeddingServiceImpl.java (implementation)
- [ ] CandidateESServiceImpl.java refactored with feature flag
- [ ] Tests with performance assertions (latency < 500ms)

## Verification Commands
```bash
# Unit tests
mvn test -Dtest=CombinedEmbeddingServiceTest

# Integration test (requires ES running)
mvn test -Dtest=CandidateESServiceIntegrationTest

# Performance test
mvn test -Dtest=CandidateESPerformanceTest
```

Expected:
- All tests PASS
- Performance test shows: Combined KNN latency < V2 latency
```

#### Files to Attach/Reference

**Must Read:**
1. `docs/improve_search/v3_hr_search/PHASE1_PLAN.md` (section 3.2)

**Current Code:**
1. `src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java`
2. `src/main/java/com/hcmute/careergraph/services/EmbedService.java`

**Models:**
1. `src/main/java/com/hcmute/careergraph/persistence/models/Job.java`

---

### Phase 1.3: Performance Testing

#### Prompt Template

```markdown
# Phase 1 Task 1.3: Performance Testing & Benchmarking

## Context
Đóng vai trò QA Engineer + Performance Tester.

V3 Phase 1 claim: P95 latency < 500ms (vs V2: 1500ms)
Goal: Prove this claim với benchmark tests.

## Setup Requirements
1. ES running with 1000+ candidates indexed
2. PostgreSQL with 50+ jobs
3. Spring Boot app running on port 8010

## Test Scenarios (from PHASE1_PLAN.md section 4.1)

### Scenario P1-001: Baseline V2 Performance
```
Config: combined-knn-enabled: false
Action: HR search with jobId (20 jobs in DB)
Measure: P50, P95, P99 latency
Expected: P95 ~ 1500ms (baseline)
```

### Scenario P1-002: V3 Combined KNN Performance
```
Config: combined-knn-enabled: true
Action: Same search as P1-001
Measure: P50, P95, P99 latency
Expected: P95 < 500ms (67% improvement)
```

### Scenario P1-003: ES Query Count
```
Config: combined-knn-enabled: true
Action: HR search with jobId
Measure: Count ES queries (use ES slow log)
Expected: 2 queries (1 BM25 + 1 KNN) vs V2: 40 queries
```

## Implementation
1. Create JMeter test plan hoặc Gatling script
2. Run 100 requests với mỗi config
3. Collect metrics: latency, throughput, ES query count
4. Generate report với comparison table

## Deliverables
- [ ] Performance test script (JMeter .jmx or Gatling .scala)
- [ ] Test results (CSV + charts)
- [ ] Report: PHASE1_PERFORMANCE_TEST_RESULTS.md

## Verification
```bash
# Run performance test
./run-phase1-performance-test.sh

# Expected output:
# V2 (combined-knn-enabled: false): P95 = 1523ms
# V3 (combined-knn-enabled: true):  P95 = 487ms
# Improvement: 68% faster ✅
```
```

---

## 🟡 Phase 2 Prompt Guide

### Phase 2.1: Implement Hybrid Scoring Backend

#### Prompt Template

```markdown
# Phase 2 Task 2.1: Implement HybridScoringService

## Context
Đóng vai trò Senior Backend Developer.

**V2 Problem:**
Min-max normalization → top candidate always 100%, even if match is POOR.
HR không biết score có ý nghĩa gì.

**V3 Solution:**
Hybrid Score = Absolute score (raw ES score) + Relative score (% in batch) + Tier + Match reasons

## Files to Read First
1. docs/improve_search/v3_hr_search/PHASE2_PLAN.md (section 2.1, 3.1)
2. docs/improve_search/v2_hr_search/V2_HR_SEARCH_IMPLEMENTATION_REPORT.md (section "Known Issues #3")
3. src/main/java/com/hcmute/careergraph/controllers/CandidateSuggestionController.java (current scoring logic)

## Implementation Steps

1. Create DTOs:
   ```java
   // HybridScore.java
   public record HybridScore(
       float absoluteScore,    // Raw ES score (0-20)
       float relativeScore,    // % in current batch (0-100)
       ScoreTier tier,         // EXCELLENT/GOOD/FAIR/POOR
       List<String> matchReasons
   ) {}
   
   // ScoreTier.java (enum)
   EXCELLENT (>= 15), GOOD (>= 10), FAIR (>= 5), POOR (< 5)
   ```

2. Create HybridScoringService:
   ```java
   public interface HybridScoringService {
       HybridScore computeScore(
           float rawScore, 
           float maxScore, 
           CandidateES candidate, 
           String searchQuery
       );
   }
   ```

3. Refactor CandidateSuggestionController:
   ```java
   // BEFORE (V2)
   float normalizedScore = (rawScore / maxScore) * 100;
   
   // AFTER (V3)
   HybridScore hybridScore = hybridScoringService.computeScore(
       rawScore, maxScore, candidate, searchQuery
   );
   response.setScore(hybridScore);  // Return full object
   ```

4. Write tests:
   - Test tier assignment (boundary cases)
   - Test match reason extraction
   - Test integration with controller

## Deliverables
- [ ] HybridScore.java (DTO)
- [ ] ScoreTier.java (enum)
- [ ] HybridScoringService interface + impl
- [ ] CandidateSuggestionController refactored
- [ ] HybridScoringServiceTest.java (10+ test cases)

## Verification
```bash
# Test endpoint
curl -X POST http://localhost:8010/careergraph/api/v1/candidate-suggestions \
  -H "Content-Type: application/json" \
  -d '{"jobId": "123", "query": "Java Spring Boot"}'

# Expected response:
{
  "candidates": [
    {
      "id": "...",
      "score": {
        "absoluteScore": 12.5,
        "relativeScore": 100,
        "tier": "GOOD",
        "matchReasons": [
          "Matches desired position: Java Backend Developer",
          "Has skill: Spring Boot (6 years)",
          "Location compatible: Ho Chi Minh City"
        ]
      }
    }
  ]
}
```
```

---

### Phase 2.2: Implement Frontend Tooltip

#### Prompt Template

```markdown
# Phase 2 Task 2.2: Create HybridScoreTooltip Component

## Context
Đóng vai trò Frontend Developer (React + TypeScript).

**Goal:** 
Khi HR hover vào score badge, show tooltip explaining:
- Absolute score với interpretation
- Relative score (position in batch)
- Tier meaning
- Top 3 match reasons

## Files to Read First
1. docs/improve_search/v3_hr_search/PHASE2_PLAN.md (section 3.2 Task 2.2)
2. careergraph-hr/src/components/candidates/CandidateCard.tsx (current score display)
3. careergraph-hr/src/components/ui/tooltip.tsx (shadcn/ui tooltip)

## Design Reference (from PHASE2_PLAN.md)
```
┌─────────────────────────────────────────────┐
│ Score Breakdown                             │
├─────────────────────────────────────────────┤
│                                             │
│ Match Strength: 12.5/20 (62%)               │
│ ████████████░░░░░░░░ GOOD                   │
│                                             │
│ Top Candidate: Ranked #1 in current search  │
│                                             │
│ Why this match?                             │
│ ✓ Desired position: Java Backend Developer │
│ ✓ Has skill: Spring Boot (6 years)         │
│ ✓ Location: Ho Chi Minh City               │
└─────────────────────────────────────────────┘
```

## Implementation Steps

1. Create component: `HybridScoreTooltip.tsx`
   ```tsx
   interface HybridScoreTooltipProps {
     score: HybridScore;
     trigger: React.ReactNode;  // Badge to wrap
   }
   ```

2. Implement tooltip content:
   - Progress bar cho absolute score
   - Tier badge với color coding
   - Match reasons list với checkmarks
   - Relative score context

3. Update CandidateCard.tsx:
   ```tsx
   // BEFORE
   <Badge>{score}%</Badge>
   
   // AFTER
   <HybridScoreTooltip score={candidate.score}>
     <Badge className={getTierColor(candidate.score.tier)}>
       {candidate.score.relativeScore}% ({candidate.score.tier})
     </Badge>
   </HybridScoreTooltip>
   ```

4. Add Storybook stories:
   - Excellent score example
   - Good score example
   - Fair score example
   - Poor score example

## Deliverables
- [ ] HybridScoreTooltip.tsx component
- [ ] HybridScoreTooltip.stories.tsx (Storybook)
- [ ] HybridScoreTooltip.test.tsx (Jest + React Testing Library)
- [ ] CandidateCard.tsx updated

## Verification
```bash
# Start Storybook
cd careergraph-hr
npm run storybook

# Navigate to: Components > HybridScoreTooltip
# Verify all 4 examples render correctly

# Run tests
npm test HybridScoreTooltip.test.tsx
```

Expected: Tooltip shows on hover, all tiers have correct colors, match reasons displayed.
```

---

## 🟡 Phase 3 Prompt Guide

### Phase 3.1: Implement Quality Validation Service

#### Prompt Template

```markdown
# Phase 3 Task 3.1-3.2: Create CvKeywordsQualityService

## Context
Đóng vai trò Senior Backend Developer + ML Engineer.

**Problem:**
Gemini extracts cvKeywords from resumeText, but sometimes hallucinates:
- resumeText: "Java Spring Boot MySQL"
- cvKeywords: "Python Django PostgreSQL" (WRONG!)

**Solution:**
Validate quality using:
1. Cosine similarity: embed(cvKeywords) vs embed(resumeText) → must be > 0.6
2. Keyword density: % of keywords appearing in resumeText → must be > 0.3
3. Overall quality = similarity × 0.7 + density × 0.3
4. If quality < 60 → auto-fallback to resumeText snippet

## Files to Read First
1. docs/improve_search/v3_hr_search/PHASE3_PLAN.md (sections 2.1, 3.1)
2. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java (extractCvKeywords method)
3. src/main/java/com/hcmute/careergraph/services/EmbedService.java (embedding service)

## Implementation Steps

1. Create DTOs:
   ```java
   // CvKeywordsQuality.java
   public record CvKeywordsQuality(
       float similarityScore,   // 0-1
       float densityScore,      // 0-1
       float overallQuality,    // 0-100
       boolean passValidation,
       QualityStatus status,    // EXCELLENT/GOOD/ACCEPTABLE/POOR/FAILED
       String fallbackReason
   ) {}
   ```

2. Implement CvKeywordsQualityService (EXACT code from PHASE3_PLAN.md section 3.1 Task 3.2):
   - computeSimilarityScore(): Cosine similarity
   - computeDensityScore(): Keyword presence check
   - validate(File file): Main validation logic

3. Integrate into CandidateESServiceImpl:
   ```java
   private String resolveValidatedCvKeywords(File file) {
       CvKeywordsQuality quality = qualityService.validate(file);
       
       if (quality.passValidation()) {
           return extractCvKeywords(file);  // Use keywords
       } else {
           log.warn("Quality failed: {}", quality.fallbackReason());
           return null;  // Fallback to resumeText
       }
   }
   ```

4. Add database column:
   ```sql
   ALTER TABLE files 
   ADD COLUMN cv_keywords_quality_json TEXT;
   ```

5. Write tests:
   - testAccurateKeywordsPassValidation()
   - testHallucinatedKeywordsFailValidation()
   - testCosineSimilarityComputation()
   - testKeywordDensityComputation()

## Deliverables
- [ ] CvKeywordsQuality.java (DTO)
- [ ] QualityStatus.java (enum)
- [ ] CvKeywordsQualityService interface + impl
- [ ] CandidateESServiceImpl integrated
- [ ] Database migration script
- [ ] CvKeywordsQualityServiceTest.java (15+ test cases)

## Verification
```bash
# Unit tests
mvn test -Dtest=CvKeywordsQualityServiceTest

# Integration test
mvn test -Dtest=CandidateESServiceIntegrationTest

# Check database
psql -d careergraph -c "SELECT id, cv_keywords_quality_json FROM files LIMIT 5;"
```

Expected:
- All tests PASS
- Database has quality JSON stored
- ES indexing uses validated keywords
```

---

### Phase 3.2: ML Threshold Optimization

#### Prompt Template

```markdown
# Phase 3 Task 3.7: Optimize Validation Thresholds

## Context
Đóng vai trò ML Engineer.

**Current thresholds (defaults):**
- min-similarity-threshold: 0.6
- min-density-threshold: 0.3

**Question:** Are these optimal? Should we use 0.65? 0.55?

**Method:** Grid search với validation dataset (100 CVs manually labeled)

## Dataset Preparation
1. Collect 100 Files with cvKeywordsJson
2. Manual labeling:
   - ACCURATE: cvKeywords match resumeText
   - HALLUCINATED: cvKeywords totally wrong
3. Split: 80 train / 20 test

## Implementation Steps

1. Export validation dataset:
   ```sql
   SELECT 
       id,
       resume_extracted_text,
       cv_keywords_json,
       -- Manual label (add column or use Google Sheets)
   FROM files
   WHERE cv_keywords_json IS NOT NULL
   LIMIT 100;
   ```

2. Create Python script: `optimize_thresholds.py`
   ```python
   # Pseudo-code from PHASE3_PLAN.md Task 3.7
   
   similarity_thresholds = [0.5, 0.55, 0.6, 0.65, 0.7]
   density_thresholds = [0.2, 0.25, 0.3, 0.35, 0.4]
   
   results = []
   for sim in similarity_thresholds:
       for dens in density_thresholds:
           # Apply thresholds
           # Compute precision, recall, F1
           results.append({...})
   
   # Find max F1
   best = max(results, key=lambda x: x['f1'])
   print(f"Optimal: sim={best['sim']}, dens={best['dens']}")
   ```

3. Run grid search:
   ```bash
   python optimize_thresholds.py --dataset validation_data.csv
   ```

4. Update application.yml với optimal values:
   ```yaml
   quality-validation:
     min-similarity-threshold: 0.62  # Optimized!
     min-density-threshold: 0.28     # Optimized!
   ```

## Deliverables
- [ ] validation_data.csv (100 samples with labels)
- [ ] optimize_thresholds.py script
- [ ] THRESHOLD_OPTIMIZATION_REPORT.md:
   - Grid search results table
   - Best thresholds
   - Precision/Recall trade-off analysis
   - Recommendation

## Verification
```bash
# Run optimization
python optimize_thresholds.py

# Expected output:
# Tested 25 combinations (5×5 grid)
# Best F1: 0.92 at sim=0.62, dens=0.28
# Precision: 0.94, Recall: 0.90
```
```

---

## ✅ Verification Checklist

### After Each Phase

- [ ] **All files created/modified as per plan**
- [ ] **Zero compilation errors** (`mvn clean compile`)
- [ ] **All tests pass** (`mvn test`)
- [ ] **Code coverage > 80%** (check with JaCoCo)
- [ ] **Application starts successfully** (`mvn spring-boot:run`)
- [ ] **Manual testing passed** (test scenarios from plan)
- [ ] **Performance targets met** (Phase 1: latency < 500ms)
- [ ] **Documentation updated** (README, JavaDoc, comments)
- [ ] **Implementation report written** (PHASE*_IMPLEMENTATION_REPORT.md)

---

## 🐛 Troubleshooting

### Issue 1: AI Agent Doesn't Have Enough Context

**Symptom:** AI creates code that doesn't match existing patterns

**Solution:**
1. Explicitly attach current code files
2. Say "Follow the exact pattern in [FileName.java]"
3. Provide code snippet examples in prompt

**Example:**
```
Current code uses this pattern:
```java
@Service
@RequiredArgsConstructor
public class MyServiceImpl implements MyService {
    private final SomeDependency dependency;
    // ...
}
```

Please follow EXACTLY this pattern for CombinedEmbeddingServiceImpl.
```

---

### Issue 2: AI Creates Tests But They Don't Run

**Symptom:** Test compilation fails or doesn't execute

**Solution:**
1. Provide existing test as template
2. Specify test framework explicitly (JUnit 5, Mockito, etc.)
3. Include pom.xml dependencies if needed

**Example:**
```
Here is an existing test as reference:
[Copy-paste HrSearchConfigTest.java]

Please create CombinedEmbeddingServiceTest.java following this exact structure:
- Use @SpringBootTest
- Use @MockBean for dependencies
- Use AssertJ assertions (assertThat(...))
```

---

### Issue 3: AI Implements But Performance Targets Not Met

**Symptom:** Phase 1 complete but P95 still > 500ms

**Solution:**
1. Ask AI to profile the code
2. Check if feature flag is enabled (`combined-knn-enabled: true`)
3. Verify ES has enough data (< 100 candidates → no performance difference)
4. Check embedding service latency

**Prompt:**
```
Phase 1 implemented nhưng P95 latency vẫn 800ms (target: < 500ms).

Hãy:
1. Add @Timed metrics to CombinedEmbeddingService methods
2. Add logging to measure each step:
   - Job sampling time
   - Text combination time
   - Embedding call time
   - ES query time
3. Run test và report which step is slow

Then suggest optimization.
```

---

### Issue 4: AI Skips Important Details from Plan

**Symptom:** Implementation works but missing features (e.g., no feature flag)

**Solution:**
Use explicit checklist in prompt:

```
Implementation MUST include:
- [ ] Feature flag: combined-knn-enabled
- [ ] Config validation (throw exception if invalid)
- [ ] Backward compatibility (V2 fallback)
- [ ] Comprehensive logging
- [ ] Unit tests + integration tests
- [ ] JavaDoc comments
- [ ] Update application.yml

Double-check each item before completing.
```

---

## 📊 Success Metrics

### How to Measure AI Agent Effectiveness

After each phase, evaluate:

1. **Correctness:** Did it implement exactly as plan? (0-100%)
2. **Completeness:** All deliverables created? (0-100%)
3. **Test Coverage:** % of code covered by tests (target: > 80%)
4. **First-Try Success:** Did it work without iteration? (yes/no)
5. **Performance:** Targets met? (yes/no)

**Good AI Prompt:**
- Correctness: > 90%
- Completeness: 100%
- Test Coverage: > 80%
- First-Try Success: Yes
- Performance: Targets met

**If not, refine prompt and retry!**

---

## 🎓 Learning from Iterations

### Prompt Improvement Log

Keep a log of what works and what doesn't:

| Prompt Version | Issue | Improvement | Result |
|----------------|-------|-------------|--------|
| v1 | AI created wrong package structure | Added explicit: "Package: com.hcmute.careergraph.config" | ✅ Fixed |
| v2 | Tests didn't mock dependencies | Provided existing test as template | ✅ Fixed |
| v3 | Missing JavaDoc | Added checklist: "Must include JavaDoc" | ✅ Fixed |

---

## 💡 Pro Tips

### 1. Use "Chain of Thought" Prompting

**Instead of:**
```
Implement Phase 1 Task 1.1
```

**Use:**
```
Before implementing, hãy:
1. Read PHASE1_PLAN.md section 3.1
2. Identify current constants in CandidateESServiceImpl.java
3. Design HrSearchConfig structure
4. List all properties to add to application.yml
5. Plan test cases

Then implement step-by-step, báo cáo sau mỗi step.
```

---

### 2. Request Self-Review

**Add to prompt:**
```
Sau khi implement xong, hãy tự review code của mình:
1. Check: Có follow code examples trong plan không?
2. Check: Có missing features nào không?
3. Check: Tests có cover edge cases không?
4. Check: Performance có đạt target không?

If any issue found, fix before submitting.
```

---

### 3. Incremental Verification

**Don't wait until end, verify after each task:**
```
# After Task 1.1
mvn clean compile  # Should PASS

# After Task 1.2
mvn test  # Should PASS

# After Task 1.3
mvn spring-boot:run  # Should START

# After Task 1.4
curl localhost:8010/health  # Should return 200
```

---

## 📞 Support

If AI agent consistently fails despite good prompts:
1. Check AI model capability (use GPT-4 / Claude 3.5, not GPT-3.5)
2. Break task into smaller subtasks
3. Provide more examples from codebase
4. Consider pair-programming: AI writes code, human reviews immediately

---

**Last Updated:** 2026-06-04  
**Version:** 1.0.0  

**Good luck with AI-assisted implementation! 🤖🚀**
