package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateSearchTextBuilder {

    private static final int RESUME_SNIPPET_CHARS = 4000;

    private final FileRepository fileRepository;

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
