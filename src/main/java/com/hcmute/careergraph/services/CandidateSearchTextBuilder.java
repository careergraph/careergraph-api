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

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSearchTextBuilder {

    private static final int RESUME_SNIPPET_CHARS = 4000;

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
     * - intentText: chỉ từ profile signals (desiredPosition, skills, industries)
     * - cvKeywords: extracted keywords từ CV (heuristic/gemini tùy strategy)
     * - embeddingText: tổng hợp cả hai để dùng cho KNN
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

        // 1. Build intentText từ profile signals
        StringBuilder intentSb = new StringBuilder();
        append(intentSb, candidate.getDesiredPosition());
        append(intentSb, candidate.getCurrentJobTitle());
        appendAll(intentSb, candidate.getIndustries());
        if (candidate.getSkills() != null) {
            candidate.getSkills().stream()
                    .map(skill -> skill.getSkill() != null ? skill.getSkill().getName() : null)
                    .forEach(value -> append(intentSb, value));
        }
        String intentText = intentSb.toString().replaceAll("\\s+", " ").trim();
        boolean hasIntent = StringUtils.hasText(intentText);

        // 2. Get CV keywords — không dùng shareToFindJob filter
        String cvKeywords = "";
        String cvKeywordsSource = "NONE";

        Optional<File> cvFileOpt = fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                candidate.getId(),
                Status.ACTIVE,
                List.of(FileType.RESUME, FileType.CV));

        if (cvFileOpt.isPresent()) {
            File cvFile = cvFileOpt.get();
            cvKeywords = resolveKeywords(cvFile);
            cvKeywordsSource = determineCvKeywordsSource(cvFile, cvKeywords);
        }

        // 3. Build embedding text (intent + keywords, tối đa 500 chars)
        String embeddingText = buildEmbeddingText(intentText, cvKeywords);

        return CandidateSearchProfile.builder()
                .intentText(intentText)
                .cvKeywords(cvKeywords)
                .embeddingText(embeddingText)
                .hasIntent(hasIntent)
                .cvKeywordsSource(cvKeywordsSource)
                .build();
    }

    private String resolveKeywords(File cvFile) {
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

    private String determineCvKeywordsSource(File cvFile, String cvKeywords) {
        if (!StringUtils.hasText(cvKeywords)) return "NONE";
        if (StringUtils.hasText(cvFile.getCvKeywordsJson())) {
            String geminiKeywords = extractSearchKeywordsFromJson(cvFile.getCvKeywordsJson());
            if (StringUtils.hasText(geminiKeywords) && geminiKeywords.equals(cvKeywords)) {
                return "GEMINI";
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
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(intentText)) {
            sb.append(intentText);
        }
        if (StringUtils.hasText(cvKeywords)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(cvKeywords);
        }
        String result = sb.toString();
        return result.length() > 500 ? result.substring(0, 500) : result;
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
