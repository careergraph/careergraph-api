package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.CvReviewRequest;
import com.hcmute.careergraph.persistence.dtos.response.CvReviewResponse;

public interface CvReviewService {

    /**
     * Review CV without job context - general CV quality assessment
     * @param candidateId the candidate requesting review
     * @param request CV data to review
     * @return Review results with score, strengths, and improvements
     */
    CvReviewResponse reviewCv(String candidateId, CvReviewRequest request);

    /**
     * Review CV with job context - assess CV fit against specific job
     * @param candidateId the candidate requesting review
     * @param jobId the job to compare CV against
     * @param request CV data to review
     * @return Review results including job fit analysis
     */
    CvReviewResponse reviewCvForJob(String candidateId, String jobId, CvReviewRequest request);
}
