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
}
