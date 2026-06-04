# Phase 2: Hybrid Scoring System (Absolute + Relative)

**Phase:** 2 of 3  
**Duration:** 2 weeks (60 hours Senior Dev + 40 hours Frontend + 40 hours QA)  
**Priority:** 🟡 MEDIUM  
**Status:** 📋 PLANNING  
**Prerequisites:** ✅ Phase 1 completed  

---

## 1. Context & Business Case

### 1.1 Current Problem (V2 Min-Max Normalization)

**Scenario từ Khách hàng khó tính:**

> "HR search 'React Developer' trong company nhỏ (5 candidates total). Top candidate:  
> - Raw ES score: 2.3  
> - Normalized score: **100%**  
>   
> HR hiểu nhầm: 'Wow, match 100%, chắc chắn phù hợp!'  
> Reality: Score 2.3 là RẤT THẤP. Candidate này chỉ mention 'React' 1 lần trong CV."

**Problem:** Min-max normalization makes top result always 100%, regardless of absolute quality.

**Impact:**
- HR waste time interviewing unqualified candidates
- Missed opportunity cost (could search wider)
- User trust issues ("Tại sao 100% match nhưng interview thấy không phù hợp?")

### 1.2 Business Goals

1. **Transparent Scoring:** HR hiểu rõ "100% trong batch yếu" vs "100% và thực sự excellent"
2. **Better Decision Making:** Reduce false positives trong candidate selection
3. **User Trust:** Build confidence in search system

### 1.3 Proposed Solution: Hybrid Scoring

```
Hybrid Score = {
    absoluteScore: float (raw ES score, 0-20+),
    relativeScore: float (0-100, normalized within batch),
    tier: enum (EXCELLENT, GOOD, FAIR, POOR),
    matchReasons: [{ field, contribution, snippet }]
}
```

**Display in UI:**
```
┌──────────────────────────────────────────────────┐
│ Candidate: John Doe                              │
│ ⭐ Match: 100% (FAIR tier)          [?] See why  │
└──────────────────────────────────────────────────┘

[Tooltip on hover [?]]
┌──────────────────────────────────────────────────┐
│ Match Quality: FAIR                              │
│ - Absolute score: 5.2 / 15+ (35%)                │
│ - Relative score: 100% (top in batch)            │
│                                                   │
│ Why this score:                                   │
│ ✓ Skills match: 40% (React, JavaScript)          │
│ ✓ Position match: 30% ("Frontend Developer")     │
│ ✓ CV keywords: 30% (UI, HTML, CSS)               │
│                                                   │
│ ⚠️ Note: This is the best match in current batch │
│    but may not be highly qualified. Consider     │
│    broadening search or refining job description.│
└──────────────────────────────────────────────────┘
```

---

## 2. Technical Design

### 2.1 Hybrid Scoring Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Hybrid Scoring Pipeline                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Elasticsearch Response                                  │
│  └─ hits: [                                              │
│      { _source: {...}, _score: 15.3, _explanation },    │
│      { _source: {...}, _score: 12.1, _explanation },    │
│      { _source: {...}, _score: 5.2, _explanation }      │
│    ]                                                     │
│          ↓                                               │
│  HybridScoringService.computeScores()                    │
│  ├─ Step 1: Extract absolute scores                     │
│  │   └─ [15.3, 12.1, 5.2]                               │
│  ├─ Step 2: Compute relative scores (min-max)           │
│  │   └─ [100%, 80%, 52%]                                │
│  ├─ Step 3: Assign tiers (threshold-based)              │
│  │   └─ [EXCELLENT, GOOD, FAIR]                         │
│  └─ Step 4: Extract match reasons (ES explain API)      │
│      └─ [reasons for each hit]                          │
│          ↓                                               │
│  CandidateSuggestionResponse                             │
│  └─ hybridScore: {                                       │
│      absoluteScore: 15.3,                               │
│      relativeScore: 100,                                │
│      tier: "EXCELLENT",                                 │
│      matchReasons: [...]                                │
│    }                                                     │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Score Tier Thresholds (Configurable via Phase 1 Config)

```yaml
application:
  hr-search:
    scoring:
      # Absolute score thresholds
      absolute-threshold-excellent: 15.0  # Top tier
      absolute-threshold-good: 10.0       # Good match
      absolute-threshold-fair: 5.0        # Acceptable
      # Below 5.0 = POOR
      
      # Hybrid weight (for combined score, future)
      hybrid-weight-absolute: 0.6   # 60% weight on absolute
      hybrid-weight-relative: 0.4   # 40% weight on relative
      
      # Match reasons
      max-match-reasons: 3          # Top 3 fields to show
```

**Tier Definition:**

| Tier | Absolute Score | Meaning | UI Color |
|------|----------------|---------|----------|
| **EXCELLENT** | ≥ 15.0 | Highly qualified, strong match | 🟢 Green |
| **GOOD** | 10.0 - 14.9 | Well qualified, good match | 🔵 Blue |
| **FAIR** | 5.0 - 9.9 | Acceptable, worth reviewing | 🟡 Yellow |
| **POOR** | < 5.0 | Weak match, consider alternatives | 🔴 Red |

---

## 3. Implementation Tasks

### 3.1 Backend Implementation (Senior Backend Developer)

#### Task 2.1: Create Hybrid Scoring Models (4 hours)

**Files to create:**
- `persistence/dtos/response/HybridScore.java`
- `persistence/dtos/response/MatchReason.java`
- `persistence/enums/ScoreTier.java`

```java
package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.persistence.enums.ScoreTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Hybrid scoring DTO for V3 Phase 2
 * Combines absolute score (ES raw) + relative score (normalized)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridScore {
    
    /**
     * Absolute score from Elasticsearch (0-20+)
     * Higher = better match regardless of batch
     */
    private float absoluteScore;
    
    /**
     * Relative score normalized within current batch (0-100)
     * Top result in batch = 100%
     */
    private float relativeScore;
    
    /**
     * Score tier based on absolute score thresholds
     * EXCELLENT (≥15), GOOD (10-14.9), FAIR (5-9.9), POOR (<5)
     */
    private ScoreTier tier;
    
    /**
     * Why this score? Top N fields that contributed
     */
    private List<MatchReason> matchReasons;
    
    /**
     * Optional: Combined score (weighted average of absolute + relative)
     * For future sorting experiments
     */
    private Float combinedScore;
}
```

```java
package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Explains why a field contributed to the match score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchReason {
    
    /**
     * Field name that matched
     * E.g., "desiredPosition", "skills", "cvKeywords"
     */
    private String field;
    
    /**
     * Display label for UI
     * E.g., "Desired Position", "Skills", "CV Keywords"
     */
    private String label;
    
    /**
     * Contribution to total score (0-100%)
     */
    private float contribution;
    
    /**
     * Matched text snippet
     * E.g., "React, TypeScript, Frontend Developer"
     */
    private String snippet;
    
    /**
     * Boost factor applied to this field
     * E.g., "^10" for desiredPosition
     */
    private String boost;
}
```

```java
package com.hcmute.careergraph.persistence.enums;

/**
 * Score tier based on absolute score thresholds
 */
public enum ScoreTier {
    EXCELLENT,  // ≥ 15.0: Highly qualified
    GOOD,       // 10.0-14.9: Well qualified
    FAIR,       // 5.0-9.9: Acceptable
    POOR        // < 5.0: Weak match
}
```

**Update CandidateSuggestionResponse:**
```java
@Data
@Builder
public class CandidateSuggestionResponse {
    // ... existing fields
    
    // V2: Simple normalized score
    // private Float score;
    
    // V3 Phase 2: Hybrid score
    private HybridScore hybridScore;  // NEW
}
```

---

#### Task 2.2: Implement HybridScoringService (16 hours)

**File:** `services/HybridScoringService.java`

```java
package com.hcmute.careergraph.services;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.dtos.response.HybridScore;
import java.util.List;

/**
 * Service for computing hybrid scores (absolute + relative)
 * Phase 2: Transparent scoring for HR
 */
public interface HybridScoringService {
    
    /**
     * Compute hybrid scores for all hits in a search response
     * 
     * @param hits List of Elasticsearch hits
     * @return List of HybridScore for each hit
     */
    List<HybridScore> computeHybridScores(List<Hit<CandidateES>> hits);
    
    /**
     * Compute hybrid score for single hit
     * 
     * @param hit Elasticsearch hit
     * @param maxScoreInBatch Max score in current batch (for relative score)
     * @return HybridScore
     */
    HybridScore computeHybridScore(Hit<CandidateES> hit, float maxScoreInBatch);
}
```

**Implementation:**
```java
package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.hcmute.careergraph.config.HrSearchConfig;
import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.dtos.response.HybridScore;
import com.hcmute.careergraph.persistence.dtos.response.MatchReason;
import com.hcmute.careergraph.persistence.enums.ScoreTier;
import com.hcmute.careergraph.services.HybridScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridScoringServiceImpl implements HybridScoringService {
    
    private final HrSearchConfig config;
    
    @Override
    public List<HybridScore> computeHybridScores(List<Hit<CandidateES>> hits) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        
        // Step 1: Find max score in batch for relative scoring
        float maxScore = hits.stream()
            .map(hit -> hit.score() != null ? hit.score().floatValue() : 0f)
            .max(Float::compare)
            .orElse(0f);
        
        // Step 2: Compute hybrid score for each hit
        return hits.stream()
            .map(hit -> computeHybridScore(hit, maxScore))
            .toList();
    }
    
    @Override
    public HybridScore computeHybridScore(Hit<CandidateES> hit, float maxScoreInBatch) {
        Double scoreValue = hit.score();
        float absoluteScore = scoreValue != null ? scoreValue.floatValue() : 0f;
        
        // Relative score: min-max normalization within batch
        float relativeScore = maxScoreInBatch > 0 
            ? (absoluteScore / maxScoreInBatch) * 100f 
            : 0f;
        
        // Tier: threshold-based on absolute score
        ScoreTier tier = determineTier(absoluteScore);
        
        // Match reasons: extract from ES explanation (or source)
        List<MatchReason> matchReasons = extractMatchReasons(hit);
        
        // Combined score (weighted average, for future sorting)
        float combinedScore = computeCombinedScore(absoluteScore, relativeScore);
        
        return HybridScore.builder()
            .absoluteScore(absoluteScore)
            .relativeScore(relativeScore)
            .tier(tier)
            .matchReasons(matchReasons)
            .combinedScore(combinedScore)
            .build();
    }
    
    /**
     * Determine tier based on configurable thresholds
     */
    private ScoreTier determineTier(float absoluteScore) {
        if (absoluteScore >= config.getScoring().getAbsoluteThresholdExcellent()) {
            return ScoreTier.EXCELLENT;
        } else if (absoluteScore >= config.getScoring().getAbsoluteThresholdGood()) {
            return ScoreTier.GOOD;
        } else if (absoluteScore >= config.getScoring().getAbsoluteThresholdFair()) {
            return ScoreTier.FAIR;
        } else {
            return ScoreTier.POOR;
        }
    }
    
    /**
     * Extract match reasons from hit source
     * Simplified version: analyze which fields have content
     * Advanced version: use ES explain API
     */
    private List<MatchReason> extractMatchReasons(Hit<CandidateES> hit) {
        List<MatchReason> reasons = new ArrayList<>();
        CandidateES source = hit.source();
        
        if (source == null) {
            return reasons;
        }
        
        // Check each field and estimate contribution
        // This is simplified; real implementation should use ES explain API
        
        if (source.getDesiredPosition() != null && !source.getDesiredPosition().isBlank()) {
            reasons.add(MatchReason.builder()
                .field("desiredPosition")
                .label("Desired Position")
                .contribution(calculateContribution("desiredPosition", source))
                .snippet(truncate(source.getDesiredPosition(), 50))
                .boost("^10")
                .build());
        }
        
        if (source.getSkills() != null && !source.getSkills().isEmpty()) {
            reasons.add(MatchReason.builder()
                .field("skills")
                .label("Skills")
                .contribution(calculateContribution("skills", source))
                .snippet(String.join(", ", source.getSkills().stream().limit(3).toList()))
                .boost("^6")
                .build());
        }
        
        if (source.getCvKeywords() != null && !source.getCvKeywords().isBlank()) {
            reasons.add(MatchReason.builder()
                .field("cvKeywords")
                .label("CV Keywords")
                .contribution(calculateContribution("cvKeywords", source))
                .snippet(truncate(source.getCvKeywords(), 50))
                .boost("^5")
                .build());
        }
        
        // Sort by contribution descending, take top N
        int maxReasons = config.getScoring().getMaxMatchReasons();
        return reasons.stream()
            .sorted((a, b) -> Float.compare(b.getContribution(), a.getContribution()))
            .limit(maxReasons)
            .toList();
    }
    
    /**
     * Estimate contribution of a field to total score
     * Simplified heuristic based on boost factors
     * Real implementation should parse ES explain API response
     */
    private float calculateContribution(String field, CandidateES source) {
        // Heuristic: contribution ≈ (boost weight / sum of all boosts) × 100
        // desiredPosition^10, skills^6, cvKeywords^5, summary^3, resumeText^1
        // Total boost: 10+6+5+3+1 = 25
        
        float totalBoost = 25f;
        
        return switch (field) {
            case "desiredPosition" -> (10f / totalBoost) * 100f;  // 40%
            case "currentJobTitle" -> (7f / totalBoost) * 100f;   // 28%
            case "skills" -> (6f / totalBoost) * 100f;             // 24%
            case "cvKeywords" -> (5f / totalBoost) * 100f;         // 20%
            case "summary" -> (3f / totalBoost) * 100f;            // 12%
            default -> 0f;
        };
    }
    
    /**
     * Compute combined score (weighted average)
     */
    private float computeCombinedScore(float absoluteScore, float relativeScore) {
        // Normalize absolute score to 0-100 scale (assume max 20)
        float normalizedAbsolute = Math.min((absoluteScore / 20f) * 100f, 100f);
        
        float weightAbsolute = config.getScoring().getHybridWeightAbsolute();
        float weightRelative = config.getScoring().getHybridWeightRelative();
        
        return (normalizedAbsolute * weightAbsolute) + (relativeScore * weightRelative);
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
```

---

#### Task 2.3: Update Controller to Use Hybrid Scoring (8 hours)

**File:** `controllers/CandidateSuggestionController.java`

```java
@PostMapping("/search")
public RestResponse<Page<CandidateSuggestionResponse>> searchCandidates(
    @RequestParam(required = false, defaultValue = "") String keyword,
    @RequestBody(required = false) CandidateFilterRequest filter,
    @RequestParam(name = "page", defaultValue = "0") Integer page,
    @RequestParam(name = "size", defaultValue = "10") Integer size,
    Authentication authentication) {
    
    // ... existing search logic
    
    // V3 Phase 2: Convert to hybrid scoring
    List<CandidateSuggestionResponse> candidates = new ArrayList<>();
    if (response != null && response.hits() != null && response.hits().hits() != null) {
        
        // Compute hybrid scores for all hits
        List<Hit<CandidateES>> hits = response.hits().hits();
        List<HybridScore> hybridScores = hybridScoringService.computeHybridScores(hits);
        
        for (int i = 0; i < hits.size(); i++) {
            Hit<CandidateES> hit = hits.get(i);
            if (hit.source() != null) {
                CandidateSuggestionResponse dto = candidateESService.toSuggestionResponse(
                    hit.source(),
                    hybridScores.get(i));  // Pass HybridScore instead of float
                candidates.add(dto);
            }
        }
    }
    
    // ... rest of the method
}
```

**Update Service Interface:**
```java
public interface CandidateESService {
    // V2: CandidateSuggestionResponse toSuggestionResponse(CandidateES candidateES, Float score);
    
    // V3 Phase 2:
    CandidateSuggestionResponse toSuggestionResponse(CandidateES candidateES, HybridScore hybridScore);
}
```

---

### 3.2 Frontend Implementation (Frontend Developer)

#### Task 2.4: Create Score Tooltip Component (12 hours)

**File:** `careergraph-hr/src/components/HybridScoreTooltip.tsx`

```typescript
import { HybridScore, MatchReason } from "@/types/hybridScore";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Badge } from "@/components/ui/badge";
import { Info } from "lucide-react";

interface HybridScoreTooltipProps {
  hybridScore: HybridScore;
}

const tierConfig = {
  EXCELLENT: { color: "bg-green-500", label: "Highly Qualified" },
  GOOD: { color: "bg-blue-500", label: "Well Qualified" },
  FAIR: { color: "bg-yellow-500", label: "Acceptable" },
  POOR: { color: "bg-red-500", label: "Weak Match" },
};

export function HybridScoreTooltip({ hybridScore }: HybridScoreTooltipProps) {
  const { absoluteScore, relativeScore, tier, matchReasons } = hybridScore;
  const config = tierConfig[tier];
  
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <button className="inline-flex items-center gap-1">
            <Badge className={config.color}>
              {Math.round(relativeScore)}%
            </Badge>
            <Info className="w-4 h-4 text-muted-foreground hover:text-foreground" />
          </button>
        </TooltipTrigger>
        
        <TooltipContent className="w-80 p-4">
          <div className="space-y-3">
            {/* Tier Badge */}
            <div>
              <Badge className={`${config.color} text-white`}>
                {tier}: {config.label}
              </Badge>
            </div>
            
            {/* Scores */}
            <div className="space-y-1 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Absolute Score:</span>
                <span className="font-medium">
                  {absoluteScore.toFixed(1)} / 15+ 
                  ({Math.round((absoluteScore / 15) * 100)}%)
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Relative Score:</span>
                <span className="font-medium">
                  {Math.round(relativeScore)}% (top in batch)
                </span>
              </div>
            </div>
            
            {/* Match Reasons */}
            <div>
              <p className="text-sm font-medium mb-2">Why this score:</p>
              <ul className="space-y-1 text-xs">
                {matchReasons.map((reason: MatchReason, idx: number) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-green-500">✓</span>
                    <div className="flex-1">
                      <span className="font-medium">{reason.label}:</span> 
                      <span className="text-muted-foreground ml-1">
                        {Math.round(reason.contribution)}%
                      </span>
                      <p className="text-muted-foreground italic">
                        "{reason.snippet}"
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
            
            {/* Warning for FAIR/POOR tiers */}
            {(tier === "FAIR" || tier === "POOR") && (
              <div className="bg-yellow-50 dark:bg-yellow-900/20 p-2 rounded text-xs">
                <p className="text-yellow-800 dark:text-yellow-200">
                  ⚠️ <strong>Note:</strong> This is the best match in current batch
                  but may not be highly qualified. Consider broadening search or
                  refining job description.
                </p>
              </div>
            )}
          </div>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
```

**Type Definitions:** `careergraph-hr/src/types/hybridScore.ts`

```typescript
export enum ScoreTier {
  EXCELLENT = "EXCELLENT",
  GOOD = "GOOD",
  FAIR = "FAIR",
  POOR = "POOR"
}

export interface MatchReason {
  field: string;
  label: string;
  contribution: number;
  snippet: string;
  boost: string;
}

export interface HybridScore {
  absoluteScore: number;
  relativeScore: number;
  tier: ScoreTier;
  matchReasons: MatchReason[];
  combinedScore?: number;
}
```

---

#### Task 2.5: Integrate Tooltip in Candidate Card (8 hours)

**File:** `careergraph-hr/src/components/CandidateCard.tsx`

```typescript
import { HybridScoreTooltip } from "@/components/HybridScoreTooltip";

export function CandidateCard({ candidate }: CandidateCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex justify-between items-start">
          <div>
            <h3>{candidate.firstName} {candidate.lastName}</h3>
            <p>{candidate.desiredPosition}</p>
          </div>
          
          {/* V3 Phase 2: Hybrid Score Tooltip */}
          <HybridScoreTooltip hybridScore={candidate.hybridScore} />
        </div>
      </CardHeader>
      
      {/* ... rest of the card */}
    </Card>
  );
}
```

---

### 3.3 Testing & QA Tasks

#### Task 2.6: A/B Testing Setup (20 hours)

**Tool:** Split.io / LaunchDarkly / Custom Feature Flag

**Experiment Config:**
```yaml
experiment:
  name: "Hybrid Scoring vs Simple Normalization"
  key: "hr-search-hybrid-scoring"
  duration: 2 weeks
  
  variants:
    - name: "control"
      description: "V2 simple normalization"
      weight: 50%
      config:
        use_hybrid_scoring: false
    
    - name: "treatment"
      description: "V3 hybrid scoring with tooltip"
      weight: 50%
      config:
        use_hybrid_scoring: true
  
  metrics:
    primary:
      - name: "HR satisfaction"
        type: "survey"
        question: "How helpful was the match score?"
        scale: 1-5
    
    secondary:
      - name: "Time per candidate"
        type: "behavioral"
        description: "Average time spent viewing candidate profile"
      
      - name: "Interview conversion"
        type: "funnel"
        description: "% candidates invited to interview"
      
      - name: "Tooltip engagement"
        type: "event"
        description: "% users who hover to see tooltip"
```

**Implementation:**
```java
@Service
public class FeatureFlagService {
    
    public boolean isHybridScoringEnabled(String userId) {
        // Integration with Split.io or LaunchDarkly
        return splitClient.getTreatment(userId, "hr-search-hybrid-scoring")
            .equals("treatment");
    }
}
```

**Controller:**
```java
@PostMapping("/search")
public RestResponse<Page<CandidateSuggestionResponse>> searchCandidates(...) {
    
    String userId = securityUtils.extractUserId(authentication);
    boolean useHybridScoring = featureFlagService.isHybridScoringEnabled(userId);
    
    if (useHybridScoring) {
        // V3: Hybrid scoring
        List<HybridScore> hybridScores = hybridScoringService.computeHybridScores(hits);
        // ...
    } else {
        // V2: Simple normalization
        float normalizedScore = (rawScore / maxScore) * 100f;
        // ...
    }
}
```

---

#### Task 2.7: User Acceptance Testing (12 hours)

**Test Scenarios:**

| Scenario | Setup | V2 Behavior | V3 Expected |
|----------|-------|-------------|-------------|
| **UAT-001** | Search "React" in small company (5 candidates, top score = 3.2) | Score: 100% | Score: 100% (POOR tier) + warning tooltip |
| **UAT-002** | Search "Java" in large company (50 candidates, top score = 18.5) | Score: 100% | Score: 100% (EXCELLENT tier) + reasons |
| **UAT-003** | Hover over score badge | No tooltip | Tooltip shows: absolute, relative, tier, reasons |
| **UAT-004** | Multiple candidates with close scores (15.2, 15.0, 14.8) | 100%, 98%, 97% | EXCELLENT, EXCELLENT, GOOD tiers visible |

**Feedback Collection:**
```
Post-test Survey:
1. How clear was the new scoring system? (1-5)
2. Did the tooltip help you understand match quality? (1-5)
3. Would you trust this scoring more than the previous version? (Yes/No/Unsure)
4. Any confusion or suggestions?
```

---

## 4. Deployment Strategy

### 4.1 Week 1-2: Development

**Day 1-3:** Tasks 2.1, 2.2 (Backend models + service)  
**Day 4-5:** Task 2.3 (Controller integration)  
**Day 6-8:** Tasks 2.4, 2.5 (Frontend tooltip + integration)  
**Day 9-10:** Task 2.6 (A/B test setup)

### 4.2 Week 3: Staging & A/B Test

**Day 1:** Deploy to staging, QA smoke tests  
**Day 2:** A/B test deployment (50/50 split)  
**Day 3-7:** Monitor metrics, collect feedback

### 4.3 Week 4: Analysis & Rollout

**Day 1-2:** Analyze A/B test results  
**Day 3:** Decision: rollout or iterate  
**Day 4-5:** Full rollout (if positive) or rollback

---

## 5. Success Criteria

### 5.1 Primary Metric

- [ ] **HR Satisfaction:** > 4.0/5.0 (vs 3.5/5.0 V2)

### 5.2 Secondary Metrics

- [ ] **Tooltip Engagement:** > 60% users hover to see details
- [ ] **Interview Conversion:** No regression (maintain ≥ 40%)
- [ ] **Time per Candidate:** No significant change (±10%)

### 5.3 Quality Metrics

- [ ] No errors in production
- [ ] Tooltip renders correctly on all screen sizes
- [ ] Accessibility: keyboard navigation works

---

## 6. Post-Implementation Report Template

**File:** `PHASE2_IMPLEMENTATION_REPORT.md`

**Sections:**
1. Executive Summary
2. A/B Test Results
   - Primary metric: HR satisfaction (before/after)
   - Secondary metrics: tooltip engagement, conversion
   - Statistical significance
3. User Feedback Summary
   - Quotes from survey
   - Common themes
4. Implementation Details
   - Backend changes
   - Frontend components
   - Configuration added
5. Deployment Log
6. Lessons Learned
7. Recommendations for Phase 3

---

**Next:** Proceed to Phase 3 (CV Keywords Quality Validation)
