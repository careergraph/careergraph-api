package com.hcmute.careergraph.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.dtos.request.CvReviewRequest;
import com.hcmute.careergraph.persistence.dtos.response.CvReviewResponse;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.CvReviewService;
import com.hcmute.careergraph.services.FastAPIClientService;
import com.hcmute.careergraph.services.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvReviewServiceImpl implements CvReviewService {

    private final FastAPIClientService fastAPIClientService;
    private final JobRepository jobRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CvReviewResponse reviewCv(String candidateId, CvReviewRequest request) {
        validateCvReviewLimit(candidateId);

        try {
            // Build request body for AI service
            String aiRequestBody = buildGeneralReviewRequest(request);

            // Call AI service
            String jsonResponse = fastAPIClientService.reviewCvBuilder(aiRequestBody);
            String cleanJson = cleanJsonString(jsonResponse);

            // Parse response
            return objectMapper.readValue(cleanJson, CvReviewResponse.class);
        } catch (Exception e) {
            log.error("Error reviewing CV for candidateId={}", candidateId, e);
            throw new BadRequestException("Failed to review CV: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CvReviewResponse reviewCvForJob(String candidateId, String jobId, CvReviewRequest request) {
        validateCvReviewLimit(candidateId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found with id: " + jobId));

        try {
            // Build request body with job context
            String aiRequestBody = buildJobFitReviewRequest(request, job);

            // Call AI service
            String jsonResponse = fastAPIClientService.reviewCvBuilder(aiRequestBody);
            String cleanJson = cleanJsonString(jsonResponse);

            // Parse response
            return objectMapper.readValue(cleanJson, CvReviewResponse.class);
        } catch (Exception e) {
            log.error("Error reviewing CV for candidateId={} jobId={}", candidateId, jobId, e);
            throw new BadRequestException("Failed to review CV: " + e.getMessage());
        }
    }

    private void validateCvReviewLimit(String candidateId) {
        String key = "cv_review_limit:" + candidateId;
        Integer current = redisService.getObject(key, Integer.class);
        if (current != null && current >= 10) {
            throw new BadRequestException("Bạn đã vượt quá số lần đánh giá CV cho phép trong ngày (tối đa 10 lần).");
        }
        redisService.setObject(key, current == null ? 1 : current + 1, 86400);
    }

    private String buildGeneralReviewRequest(CvReviewRequest request) throws Exception {
        // Create request body with CV data but no job context
        String cvDataJson = objectMapper.writeValueAsString(request.getCvData());
        return objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .put("cvData", cvDataJson)
                .put("jobContext", (String) null)
        );
    }

    private String buildJobFitReviewRequest(CvReviewRequest request, Job job) throws Exception {
        // Create request body with CV data and job context
        String jobContext = buildJobContext(job);
        String cvDataJson = objectMapper.writeValueAsString(request.getCvData());
        return objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .put("cvData", cvDataJson)
                .put("jobContext", jobContext)
        );
    }

    private String buildJobContext(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vị trí: ").append(job.getTitle() != null ? job.getTitle() : "N/A").append("\n");
        sb.append("Công ty: ").append(job.getCompany() != null ? job.getCompany().getName() : "N/A").append("\n");
        sb.append("Mô tả công việc: ").append(job.getDescription() != null ? job.getDescription() : "N/A").append("\n");
        if (job.getResponsibilities() != null && !job.getResponsibilities().isEmpty()) {
            sb.append("Trách nhiệm:\n");
            job.getResponsibilities().forEach(r -> sb.append("- ").append(r).append("\n"));
        }
        if (job.getQualifications() != null && !job.getQualifications().isEmpty()) {
            sb.append("Yêu cầu:\n");
            job.getQualifications().forEach(q -> sb.append("- ").append(q).append("\n"));
        }
        return sb.toString();
    }

    private String cleanJsonString(String response) {
        if (response.contains("```json")) {
            return response.replace("```json", "").replace("```", "").trim();
        }
        if (response.contains("```")) {
            return response.replace("```", "").trim();
        }
        return response;
    }
}
