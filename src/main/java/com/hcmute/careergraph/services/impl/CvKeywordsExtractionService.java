package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.helper.CvKeywordsHeuristicExtractor;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.services.FastAPIClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Service trích xuất CV keywords (heuristic hoặc Gemini) và lưu vào File entity.
 * Strategy: HEURISTIC | GEMINI | HYBRID (heuristic ngay, gemini async override).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvKeywordsExtractionService {

    private final FileRepository fileRepository;
    private final FastAPIClientService fastAPIClientService;
    private final CvKeywordsHeuristicExtractor heuristicExtractor;
    private final ObjectMapper objectMapper;

    @Value("${application.cv-keywords.strategy:HYBRID}")
    private String strategy;

    @Value("${application.cv-keywords.max-keywords-chars:300}")
    private int maxKeywordsChars;

    /**
     * Extract keywords và persist vào cvKeywordsJson field.
     * Gọi ngay sau khi resume text extracted thành công.
     */
    @Transactional
    public void extractAndPersistKeywords(String fileId, String resumeText) {
        if (!StringUtils.hasText(resumeText)) return;

        if ("HEURISTIC".equalsIgnoreCase(strategy)) {
            // Heuristic only: instant, no external call
            String heuristicKw = heuristicExtractor.extract(resumeText, maxKeywordsChars);
            saveKeywords(fileId, buildHeuristicJson(heuristicKw));
        } else if ("GEMINI".equalsIgnoreCase(strategy)) {
            // Gemini only: call AI service
            callGeminiAndSave(fileId, resumeText);
        } else {
            // HYBRID: heuristic first, then gemini async override
            String heuristicKw = heuristicExtractor.extract(resumeText, maxKeywordsChars);
            saveKeywords(fileId, buildHeuristicJson(heuristicKw));
            // Async Gemini call to override with better quality
            callGeminiAsync(fileId, resumeText);
        }
    }

    @Async
    public void callGeminiAsync(String fileId, String resumeText) {
        try {
            callGeminiAndSave(fileId, resumeText);
        } catch (Exception e) {
            log.warn("Async Gemini keywords extraction failed fileId={}: {}", fileId, e.getMessage());
        }
    }

    private void callGeminiAndSave(String fileId, String resumeText) {
        try {
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "resume_text", resumeText,
                    "max_keywords_chars", maxKeywordsChars
            ));
            String result = fastAPIClientService.extractCvKeywords(jsonBody);
            if (StringUtils.hasText(result)) {
                saveKeywords(fileId, result);
                log.info("Gemini CV keywords saved for fileId={}", fileId);
            }
        } catch (Exception e) {
            log.warn("Gemini CV keywords extraction failed fileId={}: {}", fileId, e.getMessage());
        }
    }

    private void saveKeywords(String fileId, String json) {
        fileRepository.findById(fileId).ifPresent(file -> {
            file.setCvKeywordsJson(json);
            fileRepository.save(file);
        });
    }

    private String buildHeuristicJson(String keywords) {
        try {
            return objectMapper.writeValueAsString(Map.of("searchKeywords", keywords, "source", "HEURISTIC"));
        } catch (Exception e) {
            return "{\"searchKeywords\":\"" + keywords.replace("\"", "'") + "\",\"source\":\"HEURISTIC\"}";
        }
    }
}
