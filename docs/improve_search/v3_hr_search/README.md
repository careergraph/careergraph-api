# V3 HR Search — Documentation Index

**Version:** 3.0.0  
**Status:** 📋 PLANNING COMPLETE  
**Created:** 2026-06-04  

---

## 📚 Document Structure

```
v3_hr_search/
├── README.md (this file)
├── V3_HR_SEARCH_MASTER_PLAN.md      ← Start here (overview)
├── PHASE1_PLAN.md                   ← Configuration & Combined KNN
├── PHASE2_PLAN.md                   ← Hybrid Scoring System
├── PHASE3_PLAN.md                   ← CV Keywords Quality Validation
│
├── AI_AGENT_PROMPT_GUIDE.md         ← 📌 How to prompt AI for implementation
├── QUICK_REFERENCE.md               ← 📌 Cheat sheet (print this!)
├── EXAMPLE_PROMPT_PHASE1_TASK1.md   ← 📌 Ready-to-use prompt example
│
└── (After implementation)
    ├── PHASE1_IMPLEMENTATION_REPORT.md
    ├── PHASE2_IMPLEMENTATION_REPORT.md
    └── PHASE3_IMPLEMENTATION_REPORT.md
```

---

## 🎯 Quick Start Guide

### For Human Developer Using AI Agent

**📖 Essential Reading:**
1. **[AI_AGENT_PROMPT_GUIDE.md](./AI_AGENT_PROMPT_GUIDE.md)** — Detailed guide on how to prompt AI for each phase
2. **[QUICK_REFERENCE.md](./QUICK_REFERENCE.md)** — Cheat sheet: print this and keep next to keyboard!
3. **[EXAMPLE_PROMPT_PHASE1_TASK1.md](./EXAMPLE_PROMPT_PHASE1_TASK1.md)** — Ready-to-use prompt (copy-paste to AI)

**⚡ Speed Run (Experienced Developer):**
1. Open [EXAMPLE_PROMPT_PHASE1_TASK1.md](./EXAMPLE_PROMPT_PHASE1_TASK1.md)
2. Copy entire content (Ctrl+A, Ctrl+C)
3. Paste into AI agent (GitHub Copilot Chat / Claude / ChatGPT)
4. Verify with commands in prompt
5. Move to next task

**🎓 Learning Path (First Time):**
1. Read AI_AGENT_PROMPT_GUIDE.md sections 1-2 (General Principles)
2. Study EXAMPLE_PROMPT_PHASE1_TASK1.md to understand structure
3. Copy prompt and run with AI
4. Iterate and learn what works
5. Create your own prompts for other tasks

---

### For AI Implementation Assistant

**Step 1:** Read [V3_HR_SEARCH_MASTER_PLAN.md](./V3_HR_SEARCH_MASTER_PLAN.md)  
→ Understand overall architecture, business goals, success criteria

**Step 2:** Read [PHASE1_PLAN.md](./PHASE1_PLAN.md)  
→ Implement configuration management + combined KNN  
→ Duration: 2 weeks  
→ Priority: 🔴 HIGH (Foundation)

**Step 3:** After Phase 1 complete, read [PHASE2_PLAN.md](./PHASE2_PLAN.md)  
→ Implement hybrid scoring system  
→ Duration: 2 weeks  
→ Priority: 🟡 MEDIUM (UX improvement)

**Step 4:** After Phase 2 complete, read [PHASE3_PLAN.md](./PHASE3_PLAN.md)  
→ Implement CV keywords quality validation  
→ Duration: 2 weeks  
→ Priority: 🟡 MEDIUM (Quality assurance)

**Step 5:** Write implementation reports after each phase

---

## 🏗️ Architecture Evolution

### V1 → V2 → V3

```
V1 (Initial):
├─ Hard-coded search logic
├─ Raw resumeText (4000 chars)
└─ Simple BM25 scoring

V2 (Current):
├─ cvKeywords extraction (Gemini)
├─ Compact job search text
├─ Min-max score normalization
└─ Issues:
    ├─ N separate KNN queries (slow)
    ├─ Hard-coded constants (inflexible)
    ├─ Misleading scores (top always 100%)
    └─ No quality validation (hallucination risk)

V3 (Planned):
├─ Phase 1: Config-driven + 1 combined KNN (fast)
├─ Phase 2: Hybrid scoring (transparent)
└─ Phase 3: Quality validation (accurate)
```

---

## 📊 Business Impact (Expected)

| Metric | V2 Baseline | V3 Target | Impact |
|--------|-------------|-----------|--------|
| **Search Latency (P95)** | 1500ms | < 500ms | -67% |
| **HR Satisfaction** | 3.5/5 | > 4.0/5 | +14% |
| **Search Precision** | 72% | > 80% | +11% |
| **False Positives** | High | -20% | -20% |
| **Infrastructure Cost** | Baseline | -50% ES queries | Cost savings |

---

## 🛠️ Tech Stack

### Backend
- **Java 17** + Spring Boot 3.4.6
- **Elasticsearch 8.x** (KNN + BM25 hybrid search)
- **PostgreSQL** (metadata storage)
- **FastAPI** (Gemini integration)

### Frontend
- **React** + TypeScript
- **shadcn/ui** (tooltip components)
- **TailwindCSS** (styling)

### DevOps
- **Kubernetes** (ConfigMap for config management)
- **Prometheus** + **Grafana** (monitoring)
- **Split.io / LaunchDarkly** (A/B testing)

---

## 👥 Roles & Responsibilities

### Phase 1
- **Senior Backend Dev** (80h): Config service, combined KNN
- **DevOps Engineer** (40h): Config deployment, monitoring
- **QA Engineer** (40h): Performance testing

### Phase 2
- **Senior Backend Dev** (60h): Hybrid scoring service
- **Frontend Developer** (40h): Tooltip UI component
- **QA Engineer** (40h): A/B testing, UAT

### Phase 3
- **Senior Backend Dev** (60h): Quality validation service
- **ML Engineer** (40h): Threshold optimization
- **QA Engineer** (40h): Quality testing

---

## 📈 Success Criteria Summary

### Phase 1 Success
✅ P95 latency < 500ms  
✅ ES query count reduced by 95%  
✅ All constants configurable  
✅ Zero errors in production  

### Phase 2 Success
✅ HR satisfaction > 4.0/5  
✅ Tooltip engagement > 60%  
✅ No regression in conversion rate  

### Phase 3 Success
✅ Validation pass rate > 95%  
✅ Search precision > 80%  
✅ Fallback trigger rate < 5%  

---

## 🚀 Deployment Timeline

```
Month 1: Phase 1
├─ Week 1-2: Development
├─ Week 3: Testing & Staging
└─ Week 4: Production Deployment

Month 2: Phase 2
├─ Week 1-2: Development
├─ Week 3: A/B Testing
└─ Week 4: Analysis & Rollout

Month 3: Phase 3
├─ Week 1-2: Development + ML Tuning
├─ Week 3: Testing & Staging
└─ Week 4: Production Deployment
```

---

## 📝 Implementation Checklist

### Pre-implementation
- [ ] Read V3_HR_SEARCH_MASTER_PLAN.md
- [ ] **Read AI_AGENT_PROMPT_GUIDE.md** (if using AI agent)
- [ ] **Print QUICK_REFERENCE.md** (cheat sheet)
- [ ] Review V2 implementation (context)
- [ ] Check prerequisites (V2 deployed, ES operational)
- [ ] Team assignments confirmed

### Phase 1
- [ ] Read PHASE1_PLAN.md
- [ ] Implement HrSearchConfig.java
- [ ] Implement CombinedEmbeddingService
- [ ] Refactor CandidateESServiceImpl
- [ ] Performance testing (target: P95 < 500ms)
- [ ] Deploy to production
- [ ] Write PHASE1_IMPLEMENTATION_REPORT.md

### Phase 2
- [ ] Read PHASE2_PLAN.md
- [ ] Implement HybridScoringService
- [ ] Create HybridScoreTooltip.tsx
- [ ] Setup A/B testing
- [ ] Collect metrics (2 weeks)
- [ ] Analyze results
- [ ] Write PHASE2_IMPLEMENTATION_REPORT.md

### Phase 3
- [ ] Read PHASE3_PLAN.md
- [ ] Implement CvKeywordsQualityService
- [ ] ML threshold optimization
- [ ] Integrate with ES indexing
- [ ] Monitor validation pass rate
- [ ] Write PHASE3_IMPLEMENTATION_REPORT.md

### Post-V3
- [ ] Final review meeting
- [ ] Lessons learned doc
- [ ] V4 roadmap planning

---

## 🔧 Configuration Reference

### application.yml (Full V3 Config)

```yaml
application:
  hr-search:
    # Phase 1: Core search parameters
    max-jobs-for-search: 20
    embedding-snippet-length: 500
    top-qualifications-count: 3
    
    # Phase 1: Combined KNN
    combined-knn-enabled: true
    combined-knn-sampling-strategy: TOP_N
    combined-knn-sample-size: 5
    combined-knn-max-length: 400
    
    # Phase 2: Scoring
    scoring:
      absolute-threshold-excellent: 15.0
      absolute-threshold-good: 10.0
      absolute-threshold-fair: 5.0
      hybrid-weight-absolute: 0.6
      hybrid-weight-relative: 0.4
      max-match-reasons: 3
    
    # Phase 3: Quality validation
    quality-validation:
      enabled: true
      min-similarity-threshold: 0.6
      min-density-threshold: 0.3
      weight-similarity: 0.7
      weight-density: 0.3
      auto-fallback-enabled: true
      fallback-quality-threshold: 60
      alert-on-low-quality-rate: 0.1
```

---

## 📊 Monitoring Dashboards

### Phase 1 Metrics
- Search latency (P50, P95, P99)
- ES query count
- Embedding API calls
- Combined KNN usage rate

### Phase 2 Metrics
- HR satisfaction score
- Tooltip engagement rate
- Interview conversion rate
- Score tier distribution

### Phase 3 Metrics
- Validation pass rate
- Average similarity score
- Average density score
- Fallback trigger rate

**Grafana Dashboard Template:** Available in each phase plan

---

## 🐛 Troubleshooting

### Common Issues

**Issue 1: Phase 1 - Combined KNN slower than V2**
- **Cause:** Combined text too long → embedding timeout
- **Fix:** Reduce `combined-knn-max-length` to 300
- **Prevention:** Monitor embedding latency

**Issue 2: Phase 2 - Tooltip not showing**
- **Cause:** Frontend route not configured
- **Fix:** Check `HybridScoreTooltip.tsx` import path
- **Prevention:** E2E frontend tests

**Issue 3: Phase 3 - Too many false negatives (good keywords flagged as bad)**
- **Cause:** Similarity threshold too high
- **Fix:** Lower `min-similarity-threshold` to 0.5
- **Prevention:** ML threshold optimization (Task 3.7)

---

## � Example Workflow: Phase 1 with AI Agent

**Scenario:** Bạn là developer, muốn AI agent implement Phase 1

### Step-by-Step

**1. Preparation (5 minutes)**
```bash
# Open workspace
cd /home/theron/Desktop/careergraph/careergraph-api

# Print cheat sheet
cat docs/improve_search/v3_hr_search/QUICK_REFERENCE.md

# Open AI agent (GitHub Copilot Chat / Claude / ChatGPT)
```

**2. First Prompt (Copy from QUICK_REFERENCE.md)**
```markdown
Đóng vai trò Senior Backend Developer 15+ năm kinh nghiệm Spring Boot.

Read these files first:
1. docs/improve_search/v3_hr_search/PHASE1_PLAN.md (section 3.1 Task 1.1)
2. src/main/java/com/hcmute/careergraph/services/impl/CandidateESServiceImpl.java
3. src/main/resources/application.yml

Implement Phase 1 Task 1.1 ONLY: Create HrSearchConfig.java

[... rest of prompt from QUICK_REFERENCE.md ...]
```

**3. AI Returns Code**
```java
// HrSearchConfig.java
// HrSearchConfigTest.java
// Updated application.yml
```

**4. Verification (You)**
```bash
# Compile
mvn clean compile
# ✅ BUILD SUCCESS

# Test
mvn test -Dtest=HrSearchConfigTest
# ✅ Tests run: 5, Failures: 0

# Check config loads
mvn spring-boot:run
# ✅ Started Application in 3.245 seconds
```

**5. Move to Task 1.2**
```
Repeat steps 2-4 for Task 1.2: CombinedEmbeddingService
```

**6. After Phase 1 Complete**
```bash
# Full test suite
mvn clean test
# ✅ All tests PASS

# Performance test
mvn test -Dtest=CandidateESPerformanceTest
# ✅ P95 latency: 487ms (target: < 500ms)

# Write report
[Create PHASE1_IMPLEMENTATION_REPORT.md]
```

**7. Sign-off**
- Tech Lead review ✅
- QA approval ✅
- Deploy to staging ✅
- Move to Phase 2 🚀

---

## �📚 References

### Internal Docs
- [V2 Implementation Report](../v2_hr_search/V2_HR_SEARCH_IMPLEMENTATION_REPORT.md)
- [V2 Job Personalization Report](../v2_job_personalization/CV_KEYWORDS_V2_IMPLEMENTATION_REPORT.md)
- [HR Search Improvement Plan](../v2_hr_search/hr-candidate-search-improvement-plan.md)

### External Resources
- [Elasticsearch KNN Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html)
- [Spring Boot ConfigurationProperties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Cosine Similarity Explained](https://en.wikipedia.org/wiki/Cosine_similarity)

---

## 💡 Tips for AI Implementation

### When Implementing Phase 1
1. **Start with config infrastructure** — foundation for everything
2. **Test combined embedding separately** before integrating
3. **Benchmark early** — validate performance improvements
4. **Keep V2 behavior as fallback** — feature flag pattern

### When Implementing Phase 2
1. **Frontend-first approach** — mock API response to develop UI
2. **Iterate tooltip design** — get user feedback early
3. **A/B test properly** — 2 weeks minimum for statistical significance
4. **Document score tier thresholds** — business decision

### When Implementing Phase 3
1. **ML threshold optimization first** — before production deployment
2. **Monitor closely** — quality validation is critical path
3. **Gradual rollout** — monitor-only → auto-fallback → full
4. **Collect ground truth data** — manual labels for validation

---

## 📖 Document Usage Matrix

**Which document should I read?**

| Your Question | Read This Document |
|---------------|-------------------|
| "What is V3 trying to achieve?" | V3_HR_SEARCH_MASTER_PLAN.md |
| "What are the known issues in V2?" | V3_HR_SEARCH_MASTER_PLAN.md section 1.2 |
| "How much will Phase 1 cost?" | V3_HR_SEARCH_MASTER_PLAN.md section 4 |
| "What exactly do I implement in Phase 1?" | PHASE1_PLAN.md |
| "What code do I write for HrSearchConfig?" | PHASE1_PLAN.md section 3.1 |
| "How do I test Phase 1?" | PHASE1_PLAN.md section 4 |
| **"How do I prompt AI to implement Phase 1?"** | **AI_AGENT_PROMPT_GUIDE.md** |
| **"Give me a ready-to-use AI prompt now!"** | **EXAMPLE_PROMPT_PHASE1_TASK1.md (copy-paste)** |
| **"What files does AI need to read?"** | **QUICK_REFERENCE.md (Files Matrix)** |
| **"What's the fastest way to prompt AI?"** | **QUICK_REFERENCE.md (Speed Run)** |
| "What if AI's code doesn't compile?" | AI_AGENT_PROMPT_GUIDE.md section 6 (Troubleshooting) |
| "How do I verify Phase 1 is complete?" | QUICK_REFERENCE.md (Verification Matrix) |
| "What are common AI prompting mistakes?" | QUICK_REFERENCE.md (Common Mistakes) |
| "What's the hybrid scoring formula?" | PHASE2_PLAN.md section 2.1 |
| "How does quality validation work?" | PHASE3_PLAN.md section 2.1 |
| "What are the deployment steps?" | PHASE[X]_PLAN.md section 4 (each phase) |
| "How do I roll back if it fails?" | PHASE[X]_PLAN.md section 6 (each phase) |

---

## 💡 Tips for AI-Assisted Implementation

### ✅ DO's
- **DO** read PHASE plan completely before prompting
- **DO** use exact code examples from plans
- **DO** verify after each task (don't batch)
- **DO** provide context files to AI (current code)
- **DO** specify exact deliverables in prompt
- **DO** ask AI to self-review before submitting
- **DO** iterate if first attempt fails

### ❌ DON'Ts
- **DON'T** prompt "implement entire phase" in one go
- **DON'T** skip verification steps
- **DON'T** accept code without tests
- **DON'T** let AI deviate from plan without reason
- **DON'T** assume AI knows your codebase patterns
- **DON'T** forget to check code coverage

---

## 🎓 Learning Resources for AI

If you're an AI reading this to implement V3:

1. **Understand the business context:**  
   Read "Known Issues" section in V2 report to understand WHY these changes

2. **Understand the architecture:**  
   Study the component diagrams in each phase plan

3. **Follow the sequence:**  
   Phase 1 → Phase 2 → Phase 3 (dependencies matter!)

4. **Test thoroughly:**  
   Each phase has detailed test cases — don't skip them

5. **Document as you go:**  
   Write implementation reports after each phase

---

## ✅ Sign-off Process

After each phase:

1. **Code Review:** Tech Lead approval
2. **QA Sign-off:** All tests passed
3. **DevOps Ready:** Monitoring configured
4. **Deploy:** Production deployment authorized
5. **Report:** Implementation report completed

---

## 📞 Support

For questions or issues:
- **Tech Lead:** [TBD]
- **Product Manager:** [TBD]
- **DevOps:** [TBD]

---

**Last Updated:** 2026-06-04  
**Document Version:** 1.0.0  
**Status:** ✅ READY FOR IMPLEMENTATION

---

**Good luck with V3 implementation! 🚀**
