package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.persistence.dtos.request.CvReviewRequest;
import com.hcmute.careergraph.persistence.dtos.response.CvReviewResponse;
import com.hcmute.careergraph.services.CvReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CvReviewController {

    private final CvReviewService cvReviewService;

    /**
     * Review CV without job context - general CV quality assessment
     * POST /api/v1/candidates/cv-review
     */
    @PostMapping("/candidates/cv-review")
    public ResponseEntity<CvReviewResponse> reviewCv(
            @RequestBody CvReviewRequest request,
            Authentication authentication) {

        String candidateId = authentication.getName();
        log.info("CV review request from candidateId={}", candidateId);

        CvReviewResponse response = cvReviewService.reviewCv(candidateId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Review CV with job context - assess CV fit against specific job
     * POST /api/v1/jobs/{jobId}/cv-review
     */
    @PostMapping("/jobs/{jobId}/cv-review")
    public ResponseEntity<CvReviewResponse> reviewCvForJob(
            @PathVariable String jobId,
            @RequestBody CvReviewRequest request,
            Authentication authentication) {

        String candidateId = authentication.getName();
        log.info("CV review request for jobId={} from candidateId={}", jobId, candidateId);

        CvReviewResponse response = cvReviewService.reviewCvForJob(candidateId, jobId, request);
        return ResponseEntity.ok(response);
    }
}
