package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.CvKeywordsHeuristicExtractor;
import com.hcmute.careergraph.persistence.dtos.response.CandidateSearchProfile;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.FileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSearchTextBuilder {

    private static final int RESUME_SNIPPET_CHARS = 4000;
    private static final int MAX_FALLBACK_CV_KEYWORDS_CHARS = 500;

    private final FileRepository fileRepository;
    private final CvKeywordsHeuristicExtractor heuristicExtractor;
    private final ObjectMapper objectMapper;

    @Value("${application.cv-keywords.strategy:HYBRID}")
    private String cvKeywordsStrategy;

    @Value("${application.cv-keywords.max-keywords-chars:300}")
    private int maxKeywordsChars;

    /**
     * Legacy method — giữ backward compatibility.
     */
    public String build(Candidate candidate, boolean includeResume) {
        if (candidate == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        append(sb, candidate.getDesiredPosition());
        append(sb, candidate.getCurrentJobTitle());
        append(sb, candidate.getSummary());
        appendAll(sb, candidate.getIndustries());
        appendAll(sb, candidate.getLocations());
        appendAll(sb, candidate.getWorkTypes());

        if (candidate.getSkills() != null) {
            candidate.getSkills().stream()
                    .map(skill -> skill.getSkill() != null ? skill.getSkill().getName() : null)
                    .forEach(value -> append(sb, value));
        }

        if (includeResume) {
            fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
                    candidate.getId(),
                    Status.ACTIVE,
                    List.of(FileType.RESUME, FileType.CV))
                    .map(File::getResumeExtractedText)
                    .filter(StringUtils::hasText)
                    .map(this::resumeSnippet)
                    .ifPresent(value -> append(sb, value));
        }

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * V2: Build structured CandidateSearchProfile cho personalized search.
     * - intentText: ưu tiên tiêu chí tìm việc và profile signals mà ứng viên chủ
     * động khai báo
     * - cvKeywords: extracted keywords từ CV (heuristic/gemini tùy strategy)
     * - embeddingText: chỉ dùng intentText nếu có, fallback sang CV keywords khi
     * thiếu intent
     */
    public CandidateSearchProfile buildProfile(Candidate candidate) {
        if (candidate == null) {
            return CandidateSearchProfile.builder()
                    .intentText("")
                    .cvKeywords("")
                    .embeddingText("")
                    .hasIntent(false)
                    .cvKeywordsSource("NONE")
                    .build();
        }

        // 1. Build intentText từ job criteria + profile signals.
        // Explicit criteria needs to lead, because CV keywords are only a cold-start
        // fallback.
        StringBuilder intentSb = new StringBuilder();
        append(intentSb, candidate.getDesiredPosition());
        // appendAll(intentSb, candidate.getIndustries());
        // appendAll(intentSb, candidate.getLocations());
        // appendAll(intentSb, candidate.getWorkTypes());
        if ((intentSb.toString().replaceAll("\\s+", " ").trim()).length() <= 0 && candidate.getSkills() != null) {
            candidate.getSkills().stream()
                    .map(skill -> skill.getSkill() != null ? skill.getSkill().getName() : null)
                    .forEach(value -> append(intentSb, value));
        }
        String intentText = intentSb.toString().replaceAll("\\s+", " ").trim();
        boolean hasIntent = StringUtils.hasText(intentText);

        // 2. Get fallback CV keywords from all active uploaded resumes — không dùng
        // shareToFindJob filter
        String cvKeywords = "";
        String cvKeywordsSource = "NONE";

        List<File> cvFiles = fileRepository.findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                candidate.getId(),
                Status.ACTIVE,
                List.of(FileType.RESUME, FileType.CV));

        if (!cvFiles.isEmpty()) {
            cvKeywords = resolveKeywords(cvFiles);
            cvKeywordsSource = determineCvKeywordsSource(cvFiles, cvKeywords);
        }

        // 3. Build search text for embedding/KNN.
        // Production rule: once intent exists, do not dilute it with CV text.
        String embeddingText = buildEmbeddingText(intentText, cvKeywords);

        return CandidateSearchProfile.builder()
                .intentText(intentText)
                .cvKeywords(cvKeywords)
                .embeddingText(embeddingText)
                .hasIntent(hasIntent)
                .cvKeywordsSource(cvKeywordsSource)
                .build();
    }

    private String resolveKeywords(List<File> cvFiles) {
        List<String> keywordParts = new ArrayList<>();
        int remainingChars = MAX_FALLBACK_CV_KEYWORDS_CHARS;

        for (File cvFile : cvFiles) {
            if (remainingChars <= 0) {
                break;
            }

            String fileKeywords = resolveKeywordsFromSingleFile(cvFile);
            if (!StringUtils.hasText(fileKeywords)) {
                continue;
            }

            String normalized = fileKeywords.replaceAll("\\s+", " ").trim();
            if (!StringUtils.hasText(normalized)) {
                continue;
            }

            if (normalized.length() > remainingChars) {
                normalized = normalized.substring(0, remainingChars).trim();
            }

            if (StringUtils.hasText(normalized)) {
                keywordParts.add(normalized);
                remainingChars -= normalized.length() + 1;
            }
        }

        return String.join(" ", keywordParts).trim();
    }

    private String resolveKeywordsFromSingleFile(File cvFile) {
        // GEMINI or HYBRID: prefer stored AI keywords if available
        if (("GEMINI".equalsIgnoreCase(cvKeywordsStrategy) || "HYBRID".equalsIgnoreCase(cvKeywordsStrategy))
                && StringUtils.hasText(cvFile.getCvKeywordsJson())) {
            String geminiKeywords = extractSearchKeywordsFromJson(cvFile.getCvKeywordsJson());
            if (StringUtils.hasText(geminiKeywords)) {
                return geminiKeywords;
            }
        }

        // HEURISTIC or HYBRID fallback: use heuristic extraction from raw text
        if (("HEURISTIC".equalsIgnoreCase(cvKeywordsStrategy) || "HYBRID".equalsIgnoreCase(cvKeywordsStrategy))
                && StringUtils.hasText(cvFile.getResumeExtractedText())) {
            return heuristicExtractor.extract(cvFile.getResumeExtractedText(), maxKeywordsChars);
        }

        return "";
    }

    private String determineCvKeywordsSource(List<File> cvFiles, String cvKeywords) {
        if (!StringUtils.hasText(cvKeywords))
            return "NONE";
        for (File cvFile : cvFiles) {
            if (StringUtils.hasText(cvFile.getCvKeywordsJson())) {
                String geminiKeywords = extractSearchKeywordsFromJson(cvFile.getCvKeywordsJson());
                if (StringUtils.hasText(geminiKeywords)) {
                    return "GEMINI";
                }
            }
        }
        return "HEURISTIC";
    }

    private String extractSearchKeywordsFromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode searchKeywords = node.get("searchKeywords");
            if (searchKeywords != null && !searchKeywords.isNull()) {
                return searchKeywords.asText("");
            }
        } catch (Exception e) {
            log.warn("Failed to parse cvKeywordsJson: {}", e.getMessage());
        }
        return "";
    }

    private String buildEmbeddingText(String intentText, String cvKeywords) {
        if (StringUtils.hasText(intentText)) {
            return truncate(intentText, 500);
        }
        return truncate(cvKeywords, 500);
    }

    private String truncate(String value, int maxChars) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() > maxChars ? value.substring(0, maxChars) : value;
    }

    private void appendAll(StringBuilder sb, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> append(sb, value));
    }

    private void append(StringBuilder sb, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(value.trim()).append(' ');
        }
    }

    private String resumeSnippet(String text) {
        return text.length() <= RESUME_SNIPPET_CHARS ? text : text.substring(0, RESUME_SNIPPET_CHARS);
    }
}
