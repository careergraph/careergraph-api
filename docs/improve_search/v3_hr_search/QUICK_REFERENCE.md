# V3 Implementation — Quick Reference Card

**Print this or keep it open while prompting AI agent!**

---

## 🎯 Universal Prompt Template

```markdown
# [Phase X Task Y]: [Task Name]

## Context
Đóng vai trò: [Senior Backend Dev / Frontend Dev / QA / ML Engineer]

Current state: [What exists now]
Goal: [What we want to achieve]
Why: [Business reason]

## Files to Read FIRST
1. docs/improve_search/v3_hr_search/PHASE[X]_PLAN.md (section [Y])
2. [Existing code file 1]
3. [Existing code file 2]

## Implementation Requirements
[Copy-paste from plan or write detailed checklist]

## Code Example (from plan)
```java
// Exact code AI should follow
```

## Deliverables Checklist
- [ ] File 1 created
- [ ] File 2 modified
- [ ] Tests written (coverage > 80%)
- [ ] Documentation updated

## Verification Commands
```bash
# Command to verify
mvn clean test -Dtest=[TestName]

# Expected output
[What should happen]
```

## Success Criteria
- [ ] Criterion 1
- [ ] Criterion 2
```

---

## 📂 Files Matrix

### Phase 1: Configuration & Combined KNN

| Task | Files to Read | Files to Create | Files to Modify |
|------|---------------|-----------------|-----------------|
| **1.1 Config** | PHASE1_PLAN.md<br>application.yml<br>CandidateESServiceImpl.java | HrSearchConfig.java<br>HrSearchConfigTest.java | application.yml |
| **1.2 Combined KNN** | PHASE1_PLAN.md<br>EmbedService.java<br>CandidateESServiceImpl.java | CombinedEmbeddingService.java<br>CombinedEmbeddingServiceImpl.java<br>CombinedEmbeddingServiceTest.java | CandidateESServiceImpl.java |
| **1.3 Performance Test** | PHASE1_PLAN.md | CandidateESPerformanceTest.java<br>PHASE1_PERFORMANCE_RESULTS.md | - |

### Phase 2: Hybrid Scoring

| Task | Files to Read | Files to Create | Files to Modify |
|------|---------------|-----------------|-----------------|
| **2.1 Backend** | PHASE2_PLAN.md<br>CandidateSuggestionController.java | HybridScore.java<br>ScoreTier.java<br>HybridScoringService.java<br>HybridScoringServiceImpl.java<br>HybridScoringServiceTest.java | CandidateSuggestionController.java |
| **2.2 Frontend** | PHASE2_PLAN.md<br>CandidateCard.tsx<br>ui/tooltip.tsx | HybridScoreTooltip.tsx<br>HybridScoreTooltip.stories.tsx<br>HybridScoreTooltip.test.tsx | CandidateCard.tsx |
| **2.3 A/B Test** | PHASE2_PLAN.md | ABTestConfig.java<br>ABTestService.java | - |

### Phase 3: Quality Validation

| Task | Files to Read | Files to Create | Files to Modify |
|------|---------------|-----------------|-----------------|
| **3.1 Models** | PHASE3_PLAN.md | CvKeywordsQuality.java<br>QualityStatus.java | File.java |
| **3.2 Service** | PHASE3_PLAN.md<br>EmbedService.java<br>CandidateESServiceImpl.java | CvKeywordsQualityService.java<br>CvKeywordsQualityServiceImpl.java<br>CvKeywordsQualityServiceTest.java | CandidateESServiceImpl.java |
| **3.3 Integration** | PHASE3_PLAN.md | - | CandidateESServiceImpl.java (resolveValidatedCvKeywords) |
| **3.4 Monitoring** | PHASE3_PLAN.md | CvKeywordsQualityMetrics.java<br>grafana-dashboard-quality.json | - |
| **3.7 ML Tuning** | PHASE3_PLAN.md | optimize_thresholds.py<br>validation_data.csv<br>THRESHOLD_OPTIMIZATION_REPORT.md | application.yml |

---

## 🔑 Key Phrases to Include in Prompts

### For Accuracy
- ✅ "Follow EXACT code example trong PHASE[X]_PLAN.md section [Y]"
- ✅ "Tuân thủ 100% existing code patterns trong [FileName.java]"
- ✅ "Copy-paste structure from [ExistingTest.java]"

### For Completeness
- ✅ "Implementation MUST include: [checklist]"
- ✅ "Double-check all deliverables before completing"
- ✅ "Verify against success criteria: [list]"

### For Testing
- ✅ "Write unit tests với coverage > 80%"
- ✅ "Include edge cases: null input, empty list, invalid config"
- ✅ "Add integration test kết nối ES và DB"

### For Performance
- ✅ "Add @Timed metrics to all service methods"
- ✅ "Target: P95 latency < [X]ms"
- ✅ "Profile và report which step is slow"

---

## 🚨 Red Flags (AI Output Quality Check)

**If AI returns code without these, reject and re-prompt:**

### Missing Required Elements
- ❌ No JavaDoc comments
- ❌ No unit tests
- ❌ No logging statements
- ❌ No null checks
- ❌ No error handling (try-catch)
- ❌ Hard-coded values (should use config)
- ❌ No feature flag (when required)

### Code Smells
- ❌ Uses deprecated methods
- ❌ Doesn't follow existing naming conventions
- ❌ Different package structure than other classes
- ❌ Uses `System.out.println` instead of logger
- ❌ Empty catch blocks
- ❌ TODO comments without JIRA ticket

### Testing Issues
- ❌ Tests don't compile
- ❌ Tests use hard-coded paths
- ❌ Tests require manual setup (not automated)
- ❌ Tests have `@Disabled` annotation
- ❌ Tests only test happy path (no edge cases)

**Action:** Point out specific issue and ask AI to fix.

---

## ⚡ Speed Run Cheat Sheet

### Phase 1 (Full Prompt in 1 Message)

```markdown
Đóng vai trò Senior Backend Developer 15+ năm kinh nghiệm Spring Boot.

Read these files first:
1. docs/improve_search/v3_hr_search/PHASE1_PLAN.md (entire file)
2. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java
3. src/main/resources/application.yml

Implement Phase 1 complete (all tasks 1.1 to 1.4):
- Task 1.1: Create HrSearchConfig.java (EXACT code from plan section 3.1)
- Task 1.2: Create CombinedEmbeddingService + impl (section 3.2)
- Task 1.3: Refactor CandidateESServiceImpl to use config + combined KNN (section 3.3)
- Task 1.4: Write comprehensive tests (section 4.1)

Requirements:
✓ Follow 100% code examples in plan
✓ Add feature flag: combined-knn-enabled
✓ Backward compatible (V2 fallback if flag = false)
✓ Unit tests + integration tests (coverage > 80%)
✓ JavaDoc all public methods
✓ Update application.yml với all properties from plan

Verification commands:
```bash
mvn clean compile  # Must PASS
mvn test           # Must PASS
mvn spring-boot:run # Must START
curl http://localhost:8010/actuator/health  # Must return 200
```

Success criteria:
- [ ] P95 latency < 500ms (vs V2: 1500ms)
- [ ] ES queries = 2 (vs V2: 40)
- [ ] All constants configurable
- [ ] Zero compilation errors

After completing, create PHASE1_PROGRESS.md listing all files created/modified.
```

### Phase 2 (Full Prompt)

```markdown
Đóng vai trò Senior Full-Stack Developer.

Read these files:
1. docs/improve_search/v3_hr_search/PHASE2_PLAN.md (entire file)
2. src/main/java/com/hcmute/careergraph/controllers/CandidateSuggestionController.java
3. careergraph-hr/src/components/candidates/CandidateCard.tsx

Implement Phase 2 complete (backend + frontend):

**Backend:**
- Create HybridScore DTO (section 2.1)
- Create HybridScoringService + impl (section 3.1)
- Refactor CandidateSuggestionController (section 3.1)
- Add config properties for score thresholds
- Write tests

**Frontend:**
- Create HybridScoreTooltip.tsx component (section 3.2)
- Update CandidateCard.tsx to use tooltip
- Create Storybook stories (4 examples: excellent/good/fair/poor)
- Write Jest tests

Requirements:
✓ Exact code from plan
✓ Score tiers: EXCELLENT (>= 15), GOOD (>= 10), FAIR (>= 5), POOR (< 5)
✓ Match reasons: top 3 only
✓ Tooltip shows on hover, accessibility compliant
✓ Tests: backend (JUnit) + frontend (Jest + RTL)

Verification:
```bash
# Backend
mvn test -Dtest=HybridScoringServiceTest

# Frontend
cd careergraph-hr && npm run storybook
npm test HybridScoreTooltip.test.tsx
```

Success: HR satisfaction > 4.0/5, tooltip engagement > 60%
```

### Phase 3 (Full Prompt)

```markdown
Đóng vai trò Senior Backend Developer + ML Engineer.

Read these files:
1. docs/improve_search/v3_hr_search/PHASE3_PLAN.md (entire file)
2. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java
3. src/main/java/com/hcmute/careergraph/services/EmbedService.java

Implement Phase 3 complete:

**Backend:**
- Create CvKeywordsQuality DTO + QualityStatus enum (section 3.1)
- Create CvKeywordsQualityService + impl (section 3.1 Task 3.2, EXACT code)
- Integrate into CandidateESServiceImpl (section 3.1 Task 3.3)
- Add quality validation metrics (section 3.1 Task 3.4)
- Update File.java entity (add cv_keywords_quality_json column)
- Write comprehensive tests

**ML:**
- Create validation dataset (100 samples)
- Create optimize_thresholds.py script (section 3.3 Task 3.7)
- Run grid search
- Update application.yml with optimal thresholds

Requirements:
✓ Cosine similarity > 0.6
✓ Keyword density > 0.3
✓ Auto-fallback if quality < 60
✓ Persist validation results to DB
✓ Prometheus metrics
✓ Tests with hallucinated keywords examples

Verification:
```bash
mvn test -Dtest=CvKeywordsQualityServiceTest
python optimize_thresholds.py --dataset validation_data.csv
```

Success: Validation pass rate > 95%, search precision +10% vs V2
```

---

## 📊 Verification Matrix

| What | Command | Expected Output |
|------|---------|-----------------|
| **Compilation** | `mvn clean compile` | BUILD SUCCESS |
| **Unit Tests** | `mvn test` | Tests run: X, Failures: 0 |
| **Coverage** | `mvn jacoco:report` | Line coverage > 80% |
| **App Starts** | `mvn spring-boot:run` | Started Application in X.XXX seconds |
| **Health Check** | `curl localhost:8010/actuator/health` | `{"status":"UP"}` |
| **API Test** | `curl -X POST localhost:8010/.../candidate-suggestions` | HTTP 200 with results |
| **Frontend** | `cd careergraph-hr && npm run dev` | Local: http://localhost:5173 |
| **Storybook** | `npm run storybook` | Storybook started on 6006 |

---

## 🎓 Common AI Prompting Mistakes

| ❌ Mistake | ✅ Better |
|-----------|----------|
| "Implement Phase 1" | "Read PHASE1_PLAN.md then implement Task 1.1 first" |
| No files referenced | "Read these files: [list]. Follow their patterns." |
| No verification specified | "After implementing, run: mvn test. All must PASS." |
| Vague requirements | "MUST include: feature flag, config, tests, JavaDoc" |
| No examples | "Copy structure from [ExistingService.java]" |
| Ask AI to "figure it out" | "Use EXACT code example from plan section 3.1.2" |
| One-shot entire phase | "Implement Task 1.1 only. I'll review before 1.2." |

---

## 🔄 Iterative Refinement Loop

```
1. Prompt AI with detailed instructions
   ↓
2. AI returns code
   ↓
3. YOU verify (compile, test, review)
   ↓
4a. If PASS → Move to next task
4b. If FAIL → Identify specific issue
   ↓
5. Re-prompt: "Line 45 has error: [message]. Fix it."
   ↓
6. AI returns fix
   ↓
7. Verify again
   ↓
8. Repeat until PASS
```

**Pro tip:** Keep iteration count < 3. If > 3, your initial prompt was too vague.

---

## 💡 Golden Rules

1. **Context is king:** AI needs to read existing code to match style
2. **Specificity wins:** "Follow EXACT code in plan" > "implement a service"
3. **Verify early:** Test after each task, not at the end
4. **Show, don't tell:** Provide code examples > explain in words
5. **Checklist everything:** AI forgets, checklists don't
6. **One task at a time:** Task 1.1 complete → verify → Task 1.2
7. **Reference plan sections:** "section 3.1.2" tells AI exactly where to look
8. **Demand tests:** No test = incomplete implementation
9. **Self-review prompt:** Ask AI to check its own work
10. **Report format:** Specify exact output format you want

---

## 📞 Emergency Shortcuts

### AI doesn't understand Spring Boot patterns?
→ Attach 2-3 existing service classes as examples

### AI creates code that doesn't compile?
→ Share the EXACT error message + stack trace

### AI's tests don't run?
→ Share an existing working test file as template

### AI is slow/stuck?
→ Break task into smaller subtasks (1 file at a time)

### AI implements wrong feature?
→ Re-read your prompt. Was it clear? Add more context.

---

## 🎯 Success Definition

**Phase implementation is complete when:**

- ✅ All deliverables created (files matrix above)
- ✅ `mvn clean test` → BUILD SUCCESS
- ✅ Coverage > 80% (check jacoco report)
- ✅ `mvn spring-boot:run` → Application starts
- ✅ Manual testing passed (test scenarios from plan)
- ✅ Performance targets met (Phase 1: < 500ms)
- ✅ Implementation report written
- ✅ Code reviewed by human (you!)
- ✅ No compiler warnings
- ✅ No TODO comments without JIRA

**Only then → proceed to next phase!**

---

**Print this card and keep it next to your keyboard! 🖨️**

**Last Updated:** 2026-06-04  
**Quick Reference v1.0**
