# Phase 1 Implementation — Example AI Prompt

**Copy-paste this into your AI agent to start Phase 1 implementation**

---

## 🎯 Phase 1 Task 1.1: Create Configuration Infrastructure

### Context

Đóng vai trò **Senior Backend Developer** với 15+ năm kinh nghiệm Spring Boot và Elasticsearch.

**Current Problem (V2):**
CandidateESServiceImpl.java có các hard-coded constants:
```java
private static final int MAX_JOBS_FOR_SEARCH = 20;
private static final int SNIPPET_LENGTH = 500;
private static final int TOP_QUALIFICATIONS_COUNT = 3;
```

**Goal:**
Externalize thành `@ConfigurationProperties` để có thể tune qua `application.yml` không cần rebuild.

### Files to Read FIRST

**Before implementing, please read these files to understand context:**

1. `docs/improve_search/v3_hr_search/PHASE1_PLAN.md` 
   - Focus on section 3.1 "Task 1.1: Create HrSearchConfig.java"
   - Read the COMPLETE code example (lines 180-350 in plan)

2. `src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java`
   - Identify all hard-coded constants (search for "= 20", "= 500", "= 3")
   - Understand current usage patterns

3. `src/main/resources/application.yml`
   - See existing configuration structure
   - You'll add new section under `application:` root

### Implementation Requirements

**Create the following files:**

#### 1. HrSearchConfig.java

**Location:** `src/main/java/com/hcmute/careergraph/config/HrSearchConfig.java`

**Requirements:**
- Use `@Configuration` + `@ConfigurationProperties(prefix = "application.hr-search")`
- Properties to include:
  ```java
  private int maxJobsForSearch = 20;
  private int embeddingSnippetLength = 500;
  private int topQualificationsCount = 3;
  private boolean combinedKnnEnabled = false;  // Feature flag
  private CombinedKnn combinedKnn = new CombinedKnn();
  private Scoring scoring = new Scoring();
  private QualityValidation qualityValidation = new QualityValidation();
  ```

- **IMPORTANT:** Follow the EXACT code structure from PHASE1_PLAN.md section 3.1
  - Include all nested classes: CombinedKnn, Scoring, QualityValidation
  - Include all validation annotations: `@Positive`, `@NotNull`, etc.
  - Include comprehensive JavaDoc comments

- Add `@PostConstruct` method to validate configuration:
  ```java
  @PostConstruct
  public void validate() {
      if (maxJobsForSearch <= 0) {
          throw new IllegalStateException("max-jobs-for-search must be positive");
      }
      // ... more validations
  }
  ```

#### 2. HrSearchConfigTest.java

**Location:** `src/test/java/com/hcmute/careergraph/config/HrSearchConfigTest.java`

**Requirements:**
- Use `@SpringBootTest`
- Test cases to include:
  ```java
  @Test
  void testDefaultValues() {
      // Verify default values are loaded correctly
      assertThat(config.getMaxJobsForSearch()).isEqualTo(20);
  }
  
  @Test
  void testCustomValues() {
      // Verify custom values from application.yml override defaults
  }
  
  @Test
  void testValidationThrowsOnInvalidConfig() {
      // Verify @PostConstruct validation works
  }
  
  @Test
  void testCombinedKnnConfig() {
      // Verify nested config (combinedKnn.enabled, etc.)
  }
  
  @Test
  void testScoringConfig() {
      // Verify scoring thresholds
  }
  ```

- Use AssertJ assertions (`assertThat(...)` not JUnit `assertEquals(...)`)
- Add JavaDoc for each test explaining what it verifies

#### 3. Update application.yml

**Location:** `src/main/resources/application.yml`

**Add this section:**
```yaml
application:
  hr-search:
    # Core search parameters
    max-jobs-for-search: 20
    embedding-snippet-length: 500
    top-qualifications-count: 3
    
    # Phase 1: Combined KNN (Feature flag)
    combined-knn-enabled: false  # Will enable after testing
    combined-knn:
      sampling-strategy: TOP_N
      sample-size: 5
      max-combined-length: 400
    
    # Phase 2: Scoring (Placeholder for future)
    scoring:
      absolute-threshold-excellent: 15.0
      absolute-threshold-good: 10.0
      absolute-threshold-fair: 5.0
      hybrid-weight-absolute: 0.6
      hybrid-weight-relative: 0.4
      max-match-reasons: 3
    
    # Phase 3: Quality validation (Placeholder for future)
    quality-validation:
      enabled: false  # Will enable in Phase 3
      min-similarity-threshold: 0.6
      min-density-threshold: 0.3
      weight-similarity: 0.7
      weight-density: 0.3
      auto-fallback-enabled: true
      fallback-quality-threshold: 60
```

### Code Style Guidelines

**Follow existing codebase patterns:**
- Package structure: `com.hcmute.careergraph.config`
- Use Lombok: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Use Spring validators: `@Positive`, `@NotNull`, `@Min`, `@Max`
- JavaDoc: Vietnamese OK, but must be comprehensive
- Logging: Use `@Slf4j` and `log.info(...)`

**Example from existing code:**
```java
@Configuration
@RequiredArgsConstructor
public class SomeExistingConfig {
    // Follow this pattern
}
```

### Deliverables Checklist

After implementation, verify you have created:

- [ ] `HrSearchConfig.java` with all properties and validation
- [ ] `HrSearchConfigTest.java` with 5+ test cases
- [ ] `application.yml` updated with all properties
- [ ] All classes have JavaDoc comments
- [ ] All public methods have JavaDoc
- [ ] Code follows existing style (Lombok, Spring validators)

### Verification Commands

**After you implement, I will run these commands to verify:**

```bash
# 1. Compilation
mvn clean compile
# Expected: BUILD SUCCESS

# 2. Unit tests
mvn test -Dtest=HrSearchConfigTest
# Expected: Tests run: 5, Failures: 0, Errors: 0

# 3. Test coverage
mvn jacoco:report
# Expected: HrSearchConfig coverage > 80%

# 4. Application starts
mvn spring-boot:run
# Expected: Started Application in X.XXX seconds
# Expected: No error logs about config validation
```

### Success Criteria

**This task is complete when:**
1. ✅ All 3 files created/modified
2. ✅ `mvn clean test` passes
3. ✅ Application starts without errors
4. ✅ Config values can be overridden via `application.yml`
5. ✅ Validation throws exception for invalid values (negative numbers)
6. ✅ Code coverage > 80% for HrSearchConfig

### Additional Notes

- **DO NOT** modify CandidateESServiceImpl.java yet (that's Task 1.3)
- **DO NOT** implement CombinedEmbeddingService yet (that's Task 1.2)
- This task is ONLY about creating the configuration infrastructure
- Focus on correctness, not optimization (yet)

### What I Expect in Your Response

Please provide:

1. **Complete code** for all 3 files (not pseudo-code)
2. **File paths** where I should create each file
3. **Brief explanation** of any design decisions you made
4. **Any assumptions** you made about existing code

**Format:**
```
File: src/main/java/com/hcmute/careergraph/config/HrSearchConfig.java
---
[Complete code here]

File: src/test/java/com/hcmute/careergraph/config/HrSearchConfigTest.java
---
[Complete code here]

File: src/main/resources/application.yml
---
[Show only the NEW section to add, not entire file]

Design decisions:
- [Decision 1]: [Reason]
- [Decision 2]: [Reason]

Assumptions:
- [Assumption 1]
- [Assumption 2]
```

---

## Ready? Let's Go! 🚀

**Proceed with implementation now.**

If you have any questions about the requirements or need clarification, ask before implementing.

If everything is clear, go ahead and create the code following the specifications above.
