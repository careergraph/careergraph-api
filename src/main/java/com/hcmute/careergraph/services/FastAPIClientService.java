package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.ChatRequest;
import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;

public interface FastAPIClientService {

    ChatResponse chat(ChatRequest request);

    String cvSuggestion(String prompt);

    /**
     * POST /api/v1/review-cv — JSON body (CvJobFitReviewRequest), trả về JSON string.
     */
    String reviewCvJobFit(String jsonBody);

    /**
     * POST /api/v1/extract-cv-keywords — Extract structured keywords từ CV text bằng Gemini.
     * Trả về JSON string hoặc null nếu thất bại.
     */
    String extractCvKeywords(String jsonBody);

    /**
     * POST /api/v1/review-cv-builder — Review CV from builder with optional job context.
     * Returns JSON string containing review results (overallScore, summary, strengths, improvements, jobFit).
     */
    String reviewCvBuilder(String jsonBody);
}
