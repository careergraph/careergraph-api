package com.hcmute.careergraph.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class CvKeywordsHeuristicExtractor {

    private static final int DEFAULT_MAX_CHARS = 300;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+|www\\.[^\\s]+|linkedin\\.com[^\\s]*|github\\.com[^\\s]*");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "\\d{1,2}/\\d{1,2}/\\d{2,4}|\\d{4}\\s*[-\u2013]\\s*(\\d{4}|nay|present|hi\u1ec7n t\u1ea1i|now)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERSONAL_LINE = Pattern.compile(
            "^\\s*(email|phone|\u0111i\u1ec7n tho\u1ea1i|s\u0111t|s\u1ed1 \u0111i\u1ec7n tho\u1ea1i|\u0111\u1ecba ch\u1ec9|address|ng\u00e0y sinh|date of birth|gi\u1edbi t\u00ednh|gender|linkedin|github|facebook|portfolio)\\s*[:\uff1a]",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final List<SectionDef> PRIORITY_SECTIONS = List.of(
            new SectionDef(Pattern.compile("(?i)(m\u1ee5c ti\u00eau|objective|career\\s*objective|summary|t\u00f3m t\u1eaft|profile|gi\u1edbi thi\u1ec7u)"), 1),
            new SectionDef(Pattern.compile("(?i)(k\u1ef9 n\u0103ng|skills|technical\\s*skills|competencies|n\u0103ng l\u1ef1c|c\u00f4ng ngh\u1ec7)"), 2),
            new SectionDef(Pattern.compile("(?i)(kinh nghi\u1ec7m|experience|work\\s*experience|professional\\s*experience)"), 3),
            new SectionDef(Pattern.compile("(?i)(h\u1ecdc v\u1ea5n|b\u1eb1ng c\u1ea5p|gi\u00e1o d\u1ee5c|education|h\u1ecdc t\u1eadp|\u0111\u1ea1i h\u1ecdc|tr\u01b0\u1eddng|school|university|degree)"), 4)
    );

    private static final Set<String> TECH_KEYWORDS = Set.of(
            "java", "python", "javascript", "typescript", "react", "angular", "vue",
            "spring", "spring boot", "node.js", "express", "django", "flask",
            "postgresql", "mysql", "mongodb", "redis", "elasticsearch",
            "docker", "kubernetes", "aws", "azure", "gcp", "ci/cd", "git",
            "microservices", "rest api", "graphql", "kafka", "rabbitmq",
            "machine learning", "deep learning", "data science", "ai",
            "html", "css", "tailwind", "figma", "devops", "linux",
            "c#", ".net", "php", "laravel", "ruby", "golang", "rust", "swift",
            "ios", "android", "flutter", "react native", "kotlin",
            "sql", "nosql", "data analyst", "business analyst", "scrum",
            "tableau", "power bi", "excel", "sap", "erp"
    );

    public String extract(String resumeText) {
        return extract(resumeText, DEFAULT_MAX_CHARS);
    }

    public String extract(String resumeText, int maxChars) {
        if (!StringUtils.hasText(resumeText)) return "";

        String cleaned = removeNoise(resumeText);
        String[] lines = cleaned.split("\\n");

        StringBuilder result = new StringBuilder();

        String firstLine = getFirstMeaningfulLine(lines);
        if (firstLine != null) {
            result.append(firstLine).append(' ');
        }

        Map<Integer, String> sectionContents = extractSections(lines);

        if (sectionContents.containsKey(1)) {
            appendWithLimit(result, sectionContents.get(1), 120, maxChars);
        }

        if (sectionContents.containsKey(2)) {
            appendWithLimit(result, sectionContents.get(2), 150, maxChars);
        }

        if (sectionContents.containsKey(3)) {
            String titles = extractJobTitlesOnly(sectionContents.get(3));
            appendWithLimit(result, titles, 100, maxChars);
        }

        String output = result.toString().trim();
        if (output.length() < 50) {
            output = getFallbackText(lines, maxChars);
        }

        return deduplicateWords(output, maxChars);
    }

    public String extractChunks(String resumeText) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> chunks = new ArrayList<>();

        if (StringUtils.hasText(resumeText)) {
            String cleaned = removeNoise(resumeText);
            String[] lines = cleaned.split("\\n");
            Map<Integer, String> sections = extractSections(lines);

            addChunk(chunks, "summary", sections.get(1), 0.8);
            addChunk(chunks, "skill", sections.get(2), 1.0);
            addChunk(chunks, "experience", sections.get(3), 0.9);
            addChunk(chunks, "education", sections.get(4), 0.6);
        }

        result.put("chunks", chunks);
        result.put("extracted_at", Instant.now().toString());
        result.put("chunk_version", 1);

        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"chunks\":[],\"extracted_at\":\"" + Instant.now() + "\",\"chunk_version\":1}";
        }
    }

    private void addChunk(List<Map<String, Object>> chunks, String type, String content, double weight) {
        if (!StringUtils.hasText(content)) {
            return;
        }

        Map<String, Object> chunk = new LinkedHashMap<>();
        chunk.put("type", type);
        chunk.put("content", content.trim());
        chunk.put("weight", weight);
        chunks.add(chunk);
    }

    private String removeNoise(String text) {
        String result = EMAIL_PATTERN.matcher(text).replaceAll("");
        result = PHONE_PATTERN.matcher(result).replaceAll("");
        result = URL_PATTERN.matcher(result).replaceAll("");
        result = DATE_RANGE_PATTERN.matcher(result).replaceAll("");
        result = PERSONAL_LINE.matcher(result).replaceAll("");
        return result;
    }

    private String getFirstMeaningfulLine(String[] lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 3) continue;
            if (trimmed.length() < 30 && trimmed.equals(trimmed.toUpperCase())
                    && !containsTechKeyword(trimmed)) {
                continue;
            }
            if (trimmed.length() >= 5) {
                return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
            }
        }
        return null;
    }

    private Map<Integer, String> extractSections(String[] lines) {
        Map<Integer, StringBuilder> sections = new HashMap<>();
        int currentPriority = -1;
        int linesSinceHeader = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            int detected = detectSectionPriority(trimmed);
            if (detected > 0) {
                currentPriority = detected;
                linesSinceHeader = 0;
                continue;
            }

            if (currentPriority > 0 && linesSinceHeader < 10) {
                sections.computeIfAbsent(currentPriority, k -> new StringBuilder())
                        .append(trimmed).append(' ');
                linesSinceHeader++;
            }
        }

        Map<Integer, String> result = new HashMap<>();
        sections.forEach((k, v) -> result.put(k, v.toString().trim()));
        return result;
    }

    private int detectSectionPriority(String line) {
        String lower = line.toLowerCase().replaceAll("[^\\p{L}\\s]", "").trim();
        for (SectionDef sp : PRIORITY_SECTIONS) {
            if (sp.pattern.matcher(lower).find() && lower.length() < 40) {
                return sp.priority;
            }
        }
        return -1;
    }

    private String extractJobTitlesOnly(String experienceContent) {
        String[] parts = experienceContent.split("\\s{2,}|\\|");
        StringBuilder titles = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.split("\\s+").length <= 8 && containsTechKeyword(trimmed)) {
                titles.append(trimmed).append(' ');
            }
        }
        return titles.toString().trim();
    }

    private boolean containsTechKeyword(String text) {
        String lower = text.toLowerCase();
        return TECH_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private void appendWithLimit(StringBuilder sb, String content, int sectionLimit, int totalLimit) {
        if (sb.length() >= totalLimit) return;
        String truncated = content.length() > sectionLimit ? content.substring(0, sectionLimit) : content;
        sb.append(truncated).append(' ');
    }

    private String getFallbackText(String[] lines, int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 3) continue;
            sb.append(trimmed).append(' ');
            if (sb.length() >= maxChars) break;
        }
        return sb.toString().trim();
    }

    private String deduplicateWords(String text, int maxChars) {
        String[] words = text.split("\\s+");
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            String lower = word.toLowerCase();
            if (seen.contains(lower)) continue;
            seen.add(lower);
            if (result.length() + word.length() + 1 > maxChars) break;
            result.append(word).append(' ');
        }
        return result.toString().trim();
    }

    private record SectionDef(Pattern pattern, int priority) {}
}
