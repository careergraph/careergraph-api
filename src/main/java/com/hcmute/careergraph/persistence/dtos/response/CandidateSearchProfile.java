package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;
import lombok.Data;

/**
 * Structured profile cho personalized job search.
 * Tách biệt intent signals (desired position, skills) và CV evidence (extracted keywords).
 * Intent signals ưu tiên cao hơn CV evidence trong BM25 ranking.
 */
@Data
@Builder
public class CandidateSearchProfile {

    /**
     * Text từ intent signals: desiredPosition, skills, industries.
     * Dùng cho BM25 text search (trọng số cao).
     */
    private String intentText;

    /**
     * Keywords extracted từ CV (heuristic hoặc Gemini).
     * Dùng bổ sung cho BM25 khi intentText thiếu hoặc trống.
     */
    private String cvKeywords;

    /**
     * Text tổng hợp intentText + cvKeywords cho embedding/KNN search.
     */
    private String embeddingText;

    /**
     * Có dữ liệu intent hay chỉ dựa CV.
     */
    private boolean hasIntent;

    /**
     * Nguồn CV keywords: GEMINI, HEURISTIC, NONE
     */
    private String cvKeywordsSource;
}
