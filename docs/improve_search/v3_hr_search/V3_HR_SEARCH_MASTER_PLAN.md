# V3 HR Search — Master Plan

**Version:** 3.0.0  
**Created:** 2026-06-04  
**Status:** 📋 PLANNING  
**Estimated Duration:** 3 Phases × 2 weeks = 6 weeks  

---

## Executive Summary

V3 HR Search giải quyết các **known issues** từ V2 implementation thông qua 3 phases:

| Phase | Focus Area | Duration | Priority |
|-------|-----------|----------|----------|
| **Phase 1** | Configuration Management & Combined KNN | 2 weeks | 🔴 HIGH |
| **Phase 2** | Hybrid Scoring System (Absolute + Relative) | 2 weeks | 🟡 MEDIUM |
| **Phase 3** | CV Keywords Quality Validation | 2 weeks | 🟡 MEDIUM |

**Total Estimated Effort:** 6 weeks (1 Senior Dev + 1 QA)

---

## Business Context

### Current Pain Points (from V2 Customer Review)

1. **Performance Issue:** N separate KNN queries cho N jobs → latency cao khi company có nhiều jobs  
   - **Impact:** Search time tăng từ 200ms → 2s khi company có 20 jobs

2. **Maintainability Issue:** Hard-coded constants scattered across codebase  
   - **Impact:** Khó tune performance cho different company sizes

3. **UX Issue:** Min-max score normalization → HR nhầm lẫn khi all candidates match yếu nhưng top = 100%  
   - **Impact:** HR waste time interview candidates không qualified

4. **Quality Issue:** Gemini có thể extract sai keywords → search results không accurate  
   - **Impact:** Candidates phù hợp bị miss, candidates không phù hợp được suggest

### Business Goals

- **Goal 1:** Reduce search latency < 500ms cho 95% requests
- **Goal 2:** Improve maintainability → config-driven parameters
- **Goal 3:** Improve HR decision quality → transparent scoring
- **Goal 4:** Maintain search precision > 80%

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      V3 HR Search Architecture                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Phase 1: Foundation Improvements                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ Config Service              Combined Embedding Service  │    │
│  │ (application.yml)           (1 KNN query thay vì N)    │    │
│  └────────────────────────────────────────────────────────┘    │
│                            ▼                                     │
│  Phase 2: Scoring Enhancement                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ Hybrid Scoring Service                                  │    │
│  │ - Absolute Score (based on threshold)                  │    │
│  │ - Relative Score (min-max normalization)               │    │
│  │ - Combined Score DTO                                    │    │
│  └────────────────────────────────────────────────────────┘    │
│                            ▼                                     │
│  Phase 3: Quality Assurance                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ CV Keywords Validation Service                          │    │
│  │ - Cosine similarity check                              │    │
│  │ - Keyword density analysis                              │    │
│  │ - Quality scoring (0-100)                               │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase Breakdown

### Phase 1: Configuration Management & Combined KNN

**Objective:** Improve performance và maintainability  
**Duration:** 2 weeks  
**Team:** 1 Senior Backend Dev + 1 DevOps  

**Deliverables:**
1. ✅ Externalize all hard-coded constants to `application.yml`
2. ✅ Implement combined embedding service cho company jobs search
3. ✅ Add configuration validation service
4. ✅ Performance benchmark report
5. ✅ Implementation report: `PHASE1_IMPLEMENTATION_REPORT.md`

**Success Criteria:**
- Search latency giảm > 50% cho no-keyword search (company jobs)
- All constants configurable via YAML
- Zero downtime deployment
- Backward compatible với V2

➡️ **Detailed Plan:** [PHASE1_PLAN.md](./PHASE1_PLAN.md)

---

### Phase 2: Hybrid Scoring System

**Objective:** Improve transparency và decision quality cho HR  
**Duration:** 2 weeks  
**Team:** 1 Senior Backend Dev + 1 Frontend Dev + 1 QA  

**Deliverables:**
1. ✅ Absolute scoring service (threshold-based)
2. ✅ Hybrid scoring DTO (absolute + relative + metadata)
3. ✅ Frontend integration: score tooltip với breakdown
4. ✅ A/B testing framework
5. ✅ Implementation report: `PHASE2_IMPLEMENTATION_REPORT.md`

**Success Criteria:**
- HR có thể phân biệt "top trong batch yếu" vs "top và thực sự qualified"
- Score tooltip hiển thị đầy đủ: absolute score, relative score, match reasons
- User acceptance testing: > 80% HR prefer V3 scoring
- No regression trong search quality metrics

➡️ **Detailed Plan:** [PHASE2_PLAN.md](./PHASE2_PLAN.md)

---

### Phase 3: CV Keywords Quality Validation

**Objective:** Ensure cvKeywords extraction quality → improve search precision  
**Duration:** 2 weeks  
**Team:** 1 Senior Backend Dev + 1 ML Engineer + 1 QA  

**Deliverables:**
1. ✅ Cosine similarity validation (cvKeywords vs resumeText)
2. ✅ Keyword density analyzer
3. ✅ Quality scoring service (0-100)
4. ✅ Auto-fallback mechanism khi quality < threshold
5. ✅ Monitoring dashboard cho extraction quality
6. ✅ Implementation report: `PHASE3_IMPLEMENTATION_REPORT.md`

**Success Criteria:**
- 95% cvKeywords có quality score > 70
- Auto-fallback trigger < 5% cases
- Search precision improvement > 10% vs V2
- Zero manual intervention needed

➡️ **Detailed Plan:** [PHASE3_PLAN.md](./PHASE3_PLAN.md)

---

## Roles & Responsibilities

### Phase 1: Configuration & Performance

| Role | Responsibilities | Time Allocation |
|------|------------------|-----------------|
| **Senior Backend Dev** | - Implement config service<br>- Refactor combined KNN<br>- Code review | 80 hours |
| **DevOps Engineer** | - Config management strategy<br>- Deployment automation<br>- Performance monitoring setup | 40 hours |
| **QA Engineer** | - Performance testing<br>- Regression testing<br>- Load testing | 40 hours |

### Phase 2: Scoring Enhancement

| Role | Responsibilities | Time Allocation |
|------|------------------|-----------------|
| **Senior Backend Dev** | - Implement hybrid scoring service<br>- API design<br>- Documentation | 60 hours |
| **Frontend Developer** | - Score tooltip UI<br>- Score breakdown visualization<br>- User feedback collection | 40 hours |
| **QA Engineer** | - A/B testing setup<br>- User acceptance testing<br>- Metrics collection | 40 hours |

### Phase 3: Quality Validation

| Role | Responsibilities | Time Allocation |
|------|------------------|-----------------|
| **Senior Backend Dev** | - Validation service implementation<br>- Fallback mechanism<br>- Integration | 60 hours |
| **ML Engineer** | - Similarity algorithms<br>- Threshold optimization<br>- Quality metrics | 40 hours |
| **QA Engineer** | - Quality validation testing<br>- Edge case testing<br>- Monitoring setup | 40 hours |

---

## Dependencies & Prerequisites

### Phase 1 Prerequisites

- [x] V2 implementation completed
- [x] Elasticsearch index `candidates_es` deployed
- [x] CvKeywordsExtractionService operational
- [ ] Performance baseline metrics collected
- [ ] Config management strategy approved

### Phase 2 Dependencies

- Phase 1 MUST be completed (needs config service)
- Frontend team availability
- A/B testing infrastructure

### Phase 3 Dependencies

- Phase 1 MUST be completed (needs config for thresholds)
- ML Engineer availability
- Monitoring infrastructure (Prometheus/Grafana)

---

## Risk Assessment

### High Risk Items

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Combined KNN performance worse than N queries | HIGH | MEDIUM | Benchmark before deployment, rollback plan |
| Frontend integration delay | MEDIUM | MEDIUM | Start API contract early, mock data |
| ML similarity threshold tuning | MEDIUM | HIGH | Use 80/20 train/test split, iterate |

### Medium Risk Items

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Config migration issues | MEDIUM | LOW | Blue-green deployment, canary release |
| User resistance to new scoring | LOW | MEDIUM | User training, documentation |
| Elasticsearch cluster downtime | HIGH | LOW | Multi-AZ deployment, backup strategy |

---

## Success Metrics (V3 vs V2)

### Performance Metrics

| Metric | V2 Baseline | V3 Target | Measurement |
|--------|-------------|-----------|-------------|
| **P95 Search Latency** | 800ms | < 500ms | Prometheus histogram |
| **P99 Search Latency** | 2000ms | < 1000ms | Prometheus histogram |
| **Elasticsearch CPU** | 60% | < 40% | Cluster metrics |
| **Memory Usage** | 4GB | < 3GB | JVM metrics |

### Quality Metrics

| Metric | V2 Baseline | V3 Target | Measurement |
|--------|-------------|-----------|-------------|
| **Search Precision** | 72% | > 80% | HR feedback survey |
| **Search Recall** | 85% | > 85% | Maintain current |
| **CV Keywords Quality** | N/A | > 70 (avg) | Auto validation score |
| **HR Satisfaction** | 3.5/5 | > 4.0/5 | User survey |

### Business Metrics

| Metric | V2 Baseline | V3 Target | Measurement |
|--------|-------------|-----------|-------------|
| **Time to hire** | 30 days | < 25 days | HR analytics |
| **Interview hit rate** | 40% | > 50% | Application tracking |
| **Cost per hire** | $5000 | < $4000 | Finance reports |

---

## Deployment Strategy

### Phase 1: Blue-Green Deployment

```
Week 1-2: Development + Unit Testing
Week 3: Staging deployment
Week 4: Production canary (10% traffic)
Week 5: Production rollout (100%)
```

### Phase 2: Feature Flag Deployment

```
Week 1-2: Development + Frontend integration
Week 3: Staging + A/B test setup
Week 4-5: Production A/B test (50% users V2, 50% V3)
Week 6: Full rollout if metrics positive
```

### Phase 3: Gradual Rollout

```
Week 1-2: Development + ML tuning
Week 3: Staging + quality metrics collection
Week 4: Production (monitor only, no fallback trigger)
Week 5: Enable auto-fallback
Week 6: Full production
```

---

## Rollback Plan

### Phase 1 Rollback Triggers

- Search latency > V2 baseline + 20%
- Error rate > 1%
- Elasticsearch cluster instability

**Rollback Steps:**
1. Feature flag OFF cho combined KNN
2. Revert to N separate queries
3. Config rollback via Kubernetes ConfigMap
4. Monitor for 1 hour
5. Post-mortem analysis

### Phase 2 Rollback Triggers

- HR satisfaction score drops > 10%
- Frontend errors > 0.5%
- API response time degradation

**Rollback Steps:**
1. Feature flag OFF cho hybrid scoring
2. Return simple normalized score
3. Revert frontend changes
4. Monitor user feedback

### Phase 3 Rollback Triggers

- Search precision drops > 5%
- Auto-fallback trigger rate > 10%
- Quality validation service errors

**Rollback Steps:**
1. Disable quality validation
2. Use all cvKeywords without check
3. Keep monitoring enabled
4. Analyze failure patterns

---

## Testing Strategy

### Phase 1: Performance Testing

**Load Testing:**
```bash
# Artillery load test config
scenarios:
  - name: "Company jobs search"
    flow:
      - post:
          url: "/candidates/suggestion/search"
          body: {}
    arrivalRate: 50  # 50 requests/sec
    duration: 300    # 5 minutes
```

**Success Criteria:**
- P95 latency < 500ms under 50 req/s
- Zero errors
- Elasticsearch query count reduced by > 50%

### Phase 2: A/B Testing

**Experiment Design:**
```yaml
experiment:
  name: "Hybrid Scoring vs Simple Normalization"
  duration: 2 weeks
  variants:
    - control: V2 (simple normalization)
      traffic: 50%
    - treatment: V3 (hybrid scoring)
      traffic: 50%
  metrics:
    primary: HR satisfaction score
    secondary: 
      - Time spent per candidate
      - Interview conversion rate
```

**Success Criteria:**
- Primary metric improvement > 10%
- No regression in secondary metrics
- User feedback positive

### Phase 3: Quality Validation

**Test Cases:**
```
1. High quality cvKeywords (similarity > 0.8):
   - Should pass validation
   - Should be used for search
   
2. Low quality cvKeywords (similarity < 0.5):
   - Should trigger fallback
   - Should use resumeText snippet
   
3. Edge case: Empty resumeText:
   - Should skip validation
   - Should use cvKeywords as-is
   
4. Edge case: Very long resumeText (> 10000 chars):
   - Should sample for validation
   - Should not timeout
```

---

## Documentation Requirements

### Developer Documentation

- [ ] API documentation (OpenAPI/Swagger)
- [ ] Architecture decision records (ADRs)
- [ ] Configuration guide
- [ ] Deployment runbook

### User Documentation

- [ ] HR user guide: New scoring system
- [ ] Admin guide: Configuration management
- [ ] Troubleshooting guide
- [ ] FAQ

### Operations Documentation

- [ ] Monitoring setup guide
- [ ] Alert configuration
- [ ] Runbook for common issues
- [ ] Performance tuning guide

---

## Timeline & Milestones

```
Month 1:
├─ Week 1-2: Phase 1 Development
├─ Week 3: Phase 1 Testing & Staging
└─ Week 4: Phase 1 Production Deployment

Month 2:
├─ Week 1-2: Phase 2 Development
├─ Week 3: Phase 2 Testing & A/B Setup
└─ Week 4: Phase 2 A/B Test Start

Month 3:
├─ Week 1: Phase 2 A/B Analysis & Rollout
├─ Week 2: Phase 3 Development Start
├─ Week 3: Phase 3 Testing & ML Tuning
└─ Week 4: Phase 3 Production Deployment

Post-Launch:
└─ Week 1-2: Monitoring & Optimization
```

---

## Cost Estimation

### Development Cost

| Phase | Personnel | Hours | Rate | Cost |
|-------|-----------|-------|------|------|
| Phase 1 | Senior Dev × 2 weeks | 80h | $100/h | $8,000 |
|         | DevOps × 1 week | 40h | $90/h | $3,600 |
|         | QA × 1 week | 40h | $70/h | $2,800 |
| **Total Phase 1** | | | | **$14,400** |
| Phase 2 | Senior Dev × 1.5 weeks | 60h | $100/h | $6,000 |
|         | Frontend Dev × 1 week | 40h | $80/h | $3,200 |
|         | QA × 1 week | 40h | $70/h | $2,800 |
| **Total Phase 2** | | | | **$12,000** |
| Phase 3 | Senior Dev × 1.5 weeks | 60h | $100/h | $6,000 |
|         | ML Engineer × 1 week | 40h | $120/h | $4,800 |
|         | QA × 1 week | 40h | $70/h | $2,800 |
| **Total Phase 3** | | | | **$13,600** |
| **Grand Total** | | | | **$40,000** |

### Infrastructure Cost (Additional)

- Elasticsearch cluster scaling: $500/month
- Monitoring (Prometheus/Grafana): $200/month
- A/B testing platform: $300/month

**Total Monthly:** $1,000

---

## Post-Implementation Review

### Week 1 Post-Launch
- Collect metrics
- User feedback survey
- Bug triage

### Week 2 Post-Launch
- Performance analysis
- Quality validation
- Optimization opportunities

### Week 4 Post-Launch
- Final report
- Lessons learned
- V4 roadmap planning

---

## Appendix

### A. Configuration Schema (Phase 1)

```yaml
application:
  hr-search:
    # Phase 1 configs
    max-jobs-for-search: 20
    embedding-snippet-length: 500
    top-qualifications-count: 3
    combined-knn-enabled: true
    
    # Phase 2 configs (future)
    scoring:
      absolute-threshold-excellent: 15.0
      absolute-threshold-good: 10.0
      absolute-threshold-fair: 5.0
      hybrid-weight-absolute: 0.6
      hybrid-weight-relative: 0.4
    
    # Phase 3 configs (future)
    quality-validation:
      enabled: true
      min-similarity-threshold: 0.6
      min-density-threshold: 0.3
      auto-fallback-enabled: true
```

### B. Hybrid Scoring DTO (Phase 2)

```java
@Data
@Builder
public class HybridScore {
    private float absoluteScore;      // 0-20+ (raw ES score)
    private float relativeScore;      // 0-100 (normalized within batch)
    private float combinedScore;      // Weighted combination
    private ScoreTier tier;           // EXCELLENT, GOOD, FAIR, POOR
    private List<MatchReason> reasons; // Why this score?
}

enum ScoreTier {
    EXCELLENT,  // absoluteScore >= 15
    GOOD,       // absoluteScore >= 10
    FAIR,       // absoluteScore >= 5
    POOR        // absoluteScore < 5
}

@Data
class MatchReason {
    private String field;       // "desiredPosition", "cvKeywords"
    private float contribution; // % contribution to total score
    private String snippet;     // Matched text snippet
}
```

### C. Quality Validation Schema (Phase 3)

```java
@Data
@Builder
public class CvKeywordsQuality {
    private float similarityScore;    // Cosine similarity vs resumeText (0-1)
    private float densityScore;       // Keyword density (0-1)
    private float overallQuality;     // Combined (0-100)
    private boolean passValidation;   // quality >= threshold
    private String fallbackReason;    // If failed, why?
}
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| **Tech Lead** | TBD | | ⏳ Pending |
| **Product Manager** | TBD | | ⏳ Pending |
| **Engineering Manager** | TBD | | ⏳ Pending |
| **DevOps Lead** | TBD | | ⏳ Pending |

---

**Next Steps:**
1. ✅ Review and approve this master plan
2. 📋 Create detailed PHASE1_PLAN.md
3. 📋 Create detailed PHASE2_PLAN.md
4. 📋 Create detailed PHASE3_PLAN.md
5. 🚀 Begin Phase 1 implementation

**Document Version:** 1.0.0  
**Last Updated:** 2026-06-04  
**Next Review:** After Phase 1 completion
