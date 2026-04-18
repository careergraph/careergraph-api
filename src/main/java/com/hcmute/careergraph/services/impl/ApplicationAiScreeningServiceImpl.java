package com.hcmute.careergraph.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationStageUpdateRequest;
import com.hcmute.careergraph.persistence.dtos.request.CvJobFitReviewRequest;
import com.hcmute.careergraph.persistence.dtos.response.CvJobFitReviewResponse;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.CandidateEducation;
import com.hcmute.careergraph.persistence.models.CandidateExperience;
import com.hcmute.careergraph.persistence.models.CandidateSkill;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.services.ApplicationAiScreeningService;
import com.hcmute.careergraph.services.ApplicationService;
import com.hcmute.careergraph.services.FastAPIClientService;
import com.hcmute.careergraph.services.NotificationService;
import com.hcmute.careergraph.services.ResumeTextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationAiScreeningServiceImpl implements ApplicationAiScreeningService {

    public static final String AI_SCREENING_ACTOR = "system:ai-screening";

    private final ApplicationRepository applicationRepository;
    private final FileRepository fileRepository;
    private final ApplicationService applicationService;
    private final FastAPIClientService fastAPIClientService;
    private final ObjectMapper objectMapper;
    private final ResumeTextExtractionService resumeTextExtractionService;
    private final NotificationService notificationService;

    @Value("${application.ai-screening.enabled:false}")
    private boolean screeningEnabled;

    @Value("${application.ai-screening.min-score-percent:50}")
    private int minScorePercent;

    @Value("${application.ai-screening.max-chars-for-ai-prompt:14000}")
    private int maxCharsForAiPrompt;

    @Override
    @Transactional
    public void screenApplication(String applicationId) {
        if (!screeningEnabled) {
            log.debug("AI screening disabled; skip applicationId={}", applicationId);
            return;
        }

        Optional<Application> opt = applicationRepository.findById(applicationId);
        if (opt.isEmpty()) {
            log.warn("AI screening: application not found id={}", applicationId);
            return;
        }

        Application application = opt.get();
        if (application.getCurrentStage() != ApplicationStage.APPLIED) {
            log.info("AI screening skip: application {} not in APPLIED (current={})",
                    applicationId, application.getCurrentStage());
            return;
        }

        Job job = application.getJob();
        Candidate candidate = application.getCandidate();
        if (job == null || candidate == null) {
            log.warn("AI screening skip: missing job or candidate on application {}", applicationId);
            return;
        }

        Hibernate.initialize(candidate.getExperiences());
        Hibernate.initialize(candidate.getEducations());

        if (StringUtils.hasText(application.getResumeUrl())) {
            resumeTextExtractionService.extractAndPersistForCandidateResumeUrl(candidate.getId(),
                    application.getResumeUrl().trim());
        }

        String candidateCvText = resolveCandidateCvTextForAi(candidate, application.getResumeUrl());

        CvJobFitReviewRequest request = CvJobFitReviewRequest.builder()
                .jobTitle(safe(job.getTitle()))
                .companyName(Optional.ofNullable(job.getCompany()).map(Company::getName).orElse(null))
                .jobDescription(safe(job.getDescription()))
                .jobQualificationsText(buildQualificationsText(job))
                .candidateProfileText(candidateCvText)
                .coverLetter(application.getCoverLetter())
                .build();

        CvJobFitReviewResponse ai;
        try {
            String json = objectMapper.writeValueAsString(request);
            String raw = fastAPIClientService.reviewCvJobFit(json);
            String clean = cleanJsonString(raw);
            ai = objectMapper.readValue(clean, CvJobFitReviewResponse.class);
        } catch (Exception ex) {
            log.error("AI screening failed for application {}: {}", applicationId, ex.getMessage(), ex);
            return;
        }

        Integer score = ai.getMatchScore();
        if (score == null) {
            log.warn("AI screening: missing matchScore for application {}", applicationId);
            return;
        }

        int clamped = Math.max(0, Math.min(100, score));
        application.setRating(clamped);
        applicationRepository.save(application);

        boolean autoRejected = clamped < minScorePercent;
        if (autoRejected) {
            String summary = StringUtils.hasText(ai.getSummary()) ? ai.getSummary().trim() : "";
            String note = buildRejectionNote(clamped, summary);
            ApplicationStageUpdateRequest stageReq = ApplicationStageUpdateRequest.builder()
                    .stage(ApplicationStage.REJECTED)
                    .note(note)
                    .changeBy(AI_SCREENING_ACTOR)
                    .build();
            applicationService.updateApplicationStage(applicationId, stageReq);
            log.info("AI screening: application {} rejected (score={} < min={})",
                    applicationId, clamped, minScorePercent);
        } else {
            log.info("AI screening: application {} passed (score={} >= min={})",
                    applicationId, clamped, minScorePercent);
        }

        String summaryForHr = StringUtils.hasText(ai.getSummary()) ? ai.getSummary().trim() : "";
        notificationService.onApplicationAiScreeningCompleted(applicationId, clamped, autoRejected, summaryForHr);
    }

    private static String buildRejectionNote(int score, String summary) {
        if (StringUtils.hasText(summary)) {
            String shortSummary = summary.length() > 220 ? summary.substring(0, 217) + "..." : summary;
            return String.format(
                    "Sàng lọc tự động (AI): độ phù hợp %d%%. %s",
                    score, shortSummary);
        }
        return String.format(
                "Sàng lọc tự động (AI): độ phù hợp %d%% — chưa đạt ngưỡng tối thiểu.",
                score);
    }

    private static String safe(String v) {
        return v != null ? v : "";
    }

    private static String buildQualificationsText(Job job) {
        StringBuilder sb = new StringBuilder();
        appendList(sb, "Yêu cầu chính", job.getQualifications());
        appendList(sb, "Yêu cầu tối thiểu", job.getMinimumQualifications());
        appendList(sb, "Trách nhiệm", job.getResponsibilities());
        String out = sb.toString().trim();
        return out.isEmpty() ? "" : out;
    }

    private static void appendList(StringBuilder sb, String label, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append(label).append(":\n");
        sb.append(items.stream().filter(StringUtils::hasText).collect(Collectors.joining("\n")));
        sb.append("\n\n");
    }

    /**
     * Ưu tiên text đã trích từ file CV ứng viên nộp (bảng {@code file}); nếu không có thì fallback profile DB.
     */
    private String resolveCandidateCvTextForAi(Candidate candidate, String resumeUrl) {
        Optional<String> fromFile = findStoredResumeExtractedText(candidate.getId(), resumeUrl);
        if (fromFile.isPresent()) {
            return truncateForAiPrompt(fromFile.get());
        }
        return buildCandidateProfileText(candidate);
    }

    private Optional<String> findStoredResumeExtractedText(String candidateId, String resumeUrl) {
        if (!StringUtils.hasText(candidateId) || !StringUtils.hasText(resumeUrl)) {
            return Optional.empty();
        }
        String raw = resumeUrl.trim();
        String normalized = normalizeResumeUrl(raw);
        Optional<File> f = fileRepository
                .findFirstByOwnerIdAndFilePathAndStatusOrderByCreatedDateDesc(candidateId, raw, Status.ACTIVE);
        if (f.isEmpty() && !raw.equals(normalized)) {
            f = fileRepository.findFirstByOwnerIdAndFilePathAndStatusOrderByCreatedDateDesc(candidateId, normalized,
                    Status.ACTIVE);
        }
        return f.map(File::getResumeExtractedText).filter(StringUtils::hasText);
    }

    private static String normalizeResumeUrl(String url) {
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private String truncateForAiPrompt(String text) {
        if (text.length() <= maxCharsForAiPrompt) {
            return text;
        }
        return text.substring(0, maxCharsForAiPrompt) + "\n...[truncated for AI prompt]";
    }

    /**
     * Fallback: profile hệ thống (khi chưa có hoặc không parse được file CV đã nộp).
     */
    private static String buildCandidateProfileText(Candidate candidate) {
        StringBuilder sb = new StringBuilder();
        sb.append("Họ tên: ")
                .append(safe(candidate.getFirstName()))
                .append(" ")
                .append(safe(candidate.getLastName()))
                .append("\n");

        if (candidate.getSkills() != null && !candidate.getSkills().isEmpty()) {
            List<String> skills = candidate.getSkills().stream()
                    .map(CandidateSkill::getSkill)
                    .filter(s -> s != null && StringUtils.hasText(s.getName()))
                    .map(s -> s.getName())
                    .toList();
            sb.append("Kỹ năng hiện có: ").append(skills).append("\n");
        }

        if (candidate.getExperiences() != null && !candidate.getExperiences().isEmpty()) {
            List<String> experiences = candidate.getExperiences().stream()
                    .filter(ex -> ex.getCompany() != null && StringUtils.hasText(ex.getCompany().getName()))
                    .map(ex -> ex.getCompany().getName() + ": from " + safe(ex.getStartDate()) + " to "
                            + safe(ex.getEndDate()))
                    .toList();
            if (!experiences.isEmpty()) {
                sb.append("Kinh nghiệm làm việc: ").append(experiences).append("\n");
            }
        }

        if (candidate.getEducations() != null && !candidate.getEducations().isEmpty()) {
            List<String> educations = candidate.getEducations().stream()
                    .filter(ed -> ed.getEducation() != null
                            && StringUtils.hasText(ed.getEducation().getOfficialName()))
                    .map(ed -> ed.getEducation().getOfficialName() + ": from " + safe(ed.getStartDate()) + " to "
                            + safe(ed.getEndDate()))
                    .toList();
            if (!educations.isEmpty()) {
                sb.append("Học vấn: ").append(educations).append("\n");
            }
        }

        if (StringUtils.hasText(candidate.getSummary())) {
            sb.append("Tóm tắt bản thân: ").append(candidate.getSummary().trim()).append("\n");
        }
        if (StringUtils.hasText(candidate.getDesiredPosition())) {
            sb.append("Vị trí mong muốn: ").append(candidate.getDesiredPosition().trim()).append("\n");
        }
        if (StringUtils.hasText(candidate.getCurrentJobTitle())) {
            sb.append("Chức danh hiện tại: ").append(candidate.getCurrentJobTitle().trim()).append("\n");
        }

        String text = sb.toString().trim();
        return text.isEmpty() ? "Không có dữ liệu profile chi tiết." : text;
    }

    private static String cleanJsonString(String response) {
        if (response == null) {
            return "{}";
        }
        if (response.contains("```json")) {
            return response.replace("```json", "").replace("```", "").trim();
        }
        if (response.contains("```")) {
            return response.replace("```", "").trim();
        }
        return response.trim();
    }
}
