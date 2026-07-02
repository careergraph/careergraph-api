package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationStageUpdateRequest;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.ApplicationService;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import com.hcmute.careergraph.services.MailService;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hcmute.careergraph.persistence.event.ApplicationCreatedEvent;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final AccountRepository accountRepository;
    private final JobRepository jobRepository;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CompanyRecruitmentStageService companyRecruitmentStageService;
    private final CompanyAccessPolicyService companyAccessPolicyService;

    private static final String SUBMISSION_NOTE = "Ứng viên đã nộp hồ sơ.";
    private static final String REAPPLY_NOTE = "Ứng viên đã nộp lại hồ sơ.";
    private static final String HR_MESSAGE_CONTACT_NOTE = "HR đã liên hệ với ứng viên qua tin nhắn.";
    private static final Set<ApplicationStage> AI_REJECTED_RECOVERY_STAGES = EnumSet.of(
            ApplicationStage.APPLIED,
            ApplicationStage.SCREENING);
    private static final Set<ApplicationStage> REAPPLY_ALLOWED_STAGES = EnumSet.of(
            ApplicationStage.APPLIED,
            ApplicationStage.REJECTED,
            ApplicationStage.OFFBOARDED);
    private static final Map<ApplicationStage, Set<ApplicationStage>> BASE_TRANSITIONS = buildBaseTransitions();

    @Override
    public Application createApplication(ApplicationRequest request) {
        log.info("Creating new application for candidate: {} to job: {}",
                request.getCandidateId(), request.getJobId());

        // Find candidate and job
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + request.getCandidateId()));

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + request.getJobId()));

        validateJobIsAcceptingApplications(job);

        LocalDateTime now = LocalDateTime.now();
        String appliedTimestamp = Optional.ofNullable(request.getAppliedDate())
                .filter(StringUtils::hasText)
                .orElse(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Optional<Application> existingApplication = findLatestApplication(request.getJobId(),
                request.getCandidateId());
        if (existingApplication.isPresent()) {
            Application existing = existingApplication.get();
            if (!REAPPLY_ALLOWED_STAGES.contains(existing.getCurrentStage())) {
                throw new BadRequestException("You have already applied for this job");
            }
            return reapplyExistingApplication(existing, request, candidate, appliedTimestamp, now);
        }

        // Create application entity
        Application application = Application.builder()
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .notes(request.getNotes())
                .appliedDate(appliedTimestamp)
                .currentStage(ApplicationStage.APPLIED)
                .stageChangedAt(now)
                .currentStageNote(SUBMISSION_NOTE)
                .candidate(candidate)
                .job(job)
                .build();

        application.addStageHistory(ApplicationStageHistory.builder()
                .fromStage(null)
                .toStage(ApplicationStage.APPLIED)
                .note(SUBMISSION_NOTE)
                .changedBy(resolveActorLabel(null, candidate))
                .changedAt(now)
                .build());

        Application savedApplication = applicationRepository.save(application);
        notificationService.onNewApplication(savedApplication);
        dispatchStageEmail(savedApplication, ApplicationStage.APPLIED, SUBMISSION_NOTE, null);
        log.info("Application created successfully with id: {}", savedApplication.getId());

        applicationEventPublisher.publishEvent(new ApplicationCreatedEvent(savedApplication.getId()));

        return savedApplication;
    }

    private Application reapplyExistingApplication(
            Application application,
            ApplicationRequest request,
            Candidate candidate,
            String appliedTimestamp,
            LocalDateTime now) {
        ApplicationStage previousStage = application.getCurrentStage();

        application.setCoverLetter(request.getCoverLetter());
        application.setResumeUrl(request.getResumeUrl());
        application.setNotes(request.getNotes());
        application.setAppliedDate(appliedTimestamp);

        if (previousStage != ApplicationStage.APPLIED) {
            application.setCurrentStage(ApplicationStage.APPLIED);
            application.setStageChangedAt(now);
            application.setCurrentStageNote(REAPPLY_NOTE);
            application.addStageHistory(ApplicationStageHistory.builder()
                    .fromStage(previousStage)
                    .toStage(ApplicationStage.APPLIED)
                    .note(REAPPLY_NOTE)
                    .changedBy(resolveActorLabel(null, candidate))
                    .changedAt(now)
                    .build());
        } else {
            application.setCurrentStageNote(SUBMISSION_NOTE);
            application.setStageChangedAt(now);
        }

        Application savedApplication = applicationRepository.save(application);
        notificationService.onNewApplication(savedApplication);
        dispatchStageEmail(savedApplication, ApplicationStage.APPLIED, REAPPLY_NOTE, null);
        log.info("Application {} re-submitted successfully", savedApplication.getId());
        applicationEventPublisher.publishEvent(new ApplicationCreatedEvent(savedApplication.getId()));
        return savedApplication;
    }

    private Optional<Application> findLatestApplication(String jobId, String candidateId) {
        return applicationRepository.findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc(candidateId, jobId);
    }

    private void validateJobIsAcceptingApplications(Job job) {
        if (job == null) {
            throw new BadRequestException("Job not found");
        }

        companyAccessPolicyService.assertJobAcceptingCandidateApplications(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Application getApplicationById(String id) {
        log.info("Getting application by id: {}", id);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        return application;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getAllApplications(String jobId, String companyId) {
        log.info("Getting all applications with pagination");

        List<Application> applications = applicationRepository.findByCompanyIdAndJobId(companyId, jobId);
        if (applications.isEmpty()) {
            return List.of();
        }
        return applications;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Application> getApplicationsByCandidate(String candidateId, Pageable pageable) {
        log.info("Getting applications by candidate id: {}", candidateId);
        return applicationRepository.findByCandidateId(candidateId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Application> getApplicationsByCandidateWithJob(String candidateId, Pageable pageable) {
        // return applicationRepository.getApplicationsByCandidateWithJob(candidateId,
        // pageable);
        return applicationRepository.findByJobId("00000000-0000-0000-0000-000000001007", pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Application> getApplicationsByCandidateWithJobWithStatus(String candidateId, Pageable pageable,
            ApplicationStage status) {
        // return applicationRepository.getApplicationsByCandidateWithJob(candidateId,
        // pageable);
        // return
        // applicationRepository.getApplicationsByCandidateWithJobWithStatus("00000000-0000-0000-0000-000000001007",
        // pageable,status);
        return applicationRepository.getAllApplicationsWithStatus(pageable, status);
    }

    @Override
    public boolean existsApplicationsByJobIdAndCandidateId(String jobId, String candidateId) {
        return applicationRepository.existsApplicationsByJobIdAndCandidateId(jobId, candidateId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApplicationReapplyBlocked(String jobId, String candidateId) {
        return findLatestApplication(jobId, candidateId)
                .map(application -> !REAPPLY_ALLOWED_STAGES.contains(application.getCurrentStage()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Application> getApplicationsByJob(String jobId, Pageable pageable) {
        log.info("Getting applications by job id: {}", jobId);
        return applicationRepository.findByJobId(jobId, pageable);
    }

    @Override
    public Application updateApplication(String id, ApplicationRequest request) {
        log.info("Updating application with id: {}", id);

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));

        // Update fields
        application.setCoverLetter(request.getCoverLetter());
        application.setResumeUrl(request.getResumeUrl());
        application.setNotes(request.getNotes());

        // Update candidate if changed
        if (!application.getCandidate().getId().equals(request.getCandidateId())) {
            Candidate candidate = candidateRepository.findById(request.getCandidateId())
                    .orElseThrow(
                            () -> new RuntimeException("Candidate not found with id: " + request.getCandidateId()));
            application.setCandidate(candidate);
        }

        // Update job if changed
        if (!application.getJob().getId().equals(request.getJobId())) {
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + request.getJobId()));
            application.setJob(job);
        }

        Application updatedApplication = applicationRepository.save(application);
        log.info("Application updated successfully with id: {}", updatedApplication.getId());

        return updatedApplication;
    }

    @Override
    public void deleteApplication(String id) {
        log.info("Deleting application with id: {}", id);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        application.softDelete();
        applicationRepository.save(application);
        log.info("Application soft deleted successfully with id: {}", id);
    }

    @Override
    public Application updateApplicationStage(String id, ApplicationStageUpdateRequest request) {
        log.info("Updating application {} stage to {}", id, request.getStage());
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));

        ApplicationStage targetStage = request.getStage();
        ApplicationStage currentStage = application.getCurrentStage();

        if (targetStage == null) {
            throw new RuntimeException("Target stage must be provided");
        }

        if (currentStage == targetStage) {
            if (StringUtils.hasText(request.getNote())) {
                application.setCurrentStageNote(request.getNote().trim());
                application.setStageChangedAt(LocalDateTime.now());
                applicationRepository.save(application);
            }
            return application;
        }

        // Enforce the curated pipeline transitions before mutating state.
        validateStageTransition(application, currentStage, targetStage, id);

        LocalDateTime now = LocalDateTime.now();
        String note = resolveStageNote(targetStage, request.getNote());
        String actor = resolveActorLabel(request.getChangeBy(), application.getCandidate());

        application.setCurrentStage(targetStage);
        application.setStageChangedAt(now);
        application.setCurrentStageNote(note);

        // Persist an auditable history record for downstream analytics.
        application.addStageHistory(ApplicationStageHistory.builder()
                .fromStage(currentStage)
                .toStage(targetStage)
                .note(note)
                .changedBy(actor)
                .changedAt(now)
                .build());

        Application saved = applicationRepository.save(application);
        Account changedByAccount = StringUtils.hasText(request.getChangeBy())
                ? accountRepository.findByCompanyId(request.getChangeBy()).orElse(null)
                : null;
        notificationService.onApplicationStatusChanged(saved, currentStage, targetStage, changedByAccount);
        dispatchStageEmail(saved, targetStage, note, actor);
        log.info("Application {} moved from {} to {}", id, currentStage, targetStage);
        return saved;
    }

    @Override
    public void promoteToHrContactedOnHrMessage(String applicationId, Account hrAccount) {
        if (!StringUtils.hasText(applicationId)) {
            return;
        }

        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            return;
        }

        ApplicationStage currentStage = application.getCurrentStage();
        if (currentStage != ApplicationStage.APPLIED && currentStage != ApplicationStage.SCREENING) {
            return;
        }

        ApplicationStageUpdateRequest request = new ApplicationStageUpdateRequest();
        request.setStage(ApplicationStage.HR_CONTACTED);
        request.setNote(HR_MESSAGE_CONTACT_NOTE);
        if (hrAccount != null && hrAccount.getCompany() != null) {
            request.setChangeBy(hrAccount.getCompany().getId());
        }

        updateApplicationStage(applicationId, request);
    }

    private void validateStageTransition(
            Application application,
            ApplicationStage currentStage,
            ApplicationStage targetStage,
            String applicationId) {
        if (currentStage == null) {
            return;
        }
        if (currentStage == targetStage) {
            return;
        }

        Company company = Optional.ofNullable(application)
                .map(Application::getJob)
                .map(Job::getCompany)
                .orElse(null);

        if (targetStage == ApplicationStage.REJECTED) {
            validateHrContactedStageActive(company, targetStage);
            return;
        }

        if (currentStage == ApplicationStage.REJECTED) {
            validateAiRejectedRecovery(application, company, targetStage, applicationId);
            return;
        }

        if (currentStage.isTerminal()) {
            throw new RuntimeException(String.format(
                    "Stage transition from %s to %s is not allowed for application %s",
                    currentStage, targetStage, applicationId));
        }

        if (isHrMessagingPromotion(currentStage, targetStage)) {
            validateHrContactedStageActive(company, targetStage);
            return;
        }

        if (isAiScreeningPromotion(currentStage, targetStage)) {
            validateHrContactedStageActive(company, targetStage);
            return;
        }

        if (company != null) {
            List<CompanyRecruitmentStage> stages = companyRecruitmentStageService.getCompanyStages(company.getId());
            ApplicationStage nextActiveStage = resolveNextActiveStage(stages, currentStage);
            if (nextActiveStage == null || nextActiveStage != targetStage) {
                throw new RuntimeException(String.format(
                        "Stage transition from %s to %s is not allowed for application %s",
                        currentStage, targetStage, applicationId));
            }
        } else {
            Set<ApplicationStage> allowedTargets = BASE_TRANSITIONS.getOrDefault(currentStage, Set.of());
            if (!allowedTargets.contains(targetStage)) {
                throw new RuntimeException(String.format(
                        "Stage transition from %s to %s is not allowed for application %s",
                        currentStage, targetStage, applicationId));
            }
        }

        boolean offerBeforeTrial = company == null || !Boolean.FALSE.equals(company.getOfferBeforeTrial());

        if (company != null && ApplicationStage.isConfigurableStage(targetStage)) {
            boolean activeStage = companyRecruitmentStageService
                    .isStageActiveForCompany(company.getId(), targetStage);
            if (!activeStage) {
                throw new RuntimeException("Stage hiện đã bị tắt trong pipeline của công ty");
            }
        }

        if (offerBeforeTrial
                && currentStage == ApplicationStage.INTERVIEW_COMPLETED
                && targetStage == ApplicationStage.TRIAL) {
            throw new RuntimeException("Company pipeline yêu cầu gửi Offer trước khi chuyển sang Thử việc");
        }

        if (!offerBeforeTrial
                && currentStage == ApplicationStage.INTERVIEW_COMPLETED
                && targetStage == ApplicationStage.OFFER_EXTENDED) {
            throw new RuntimeException("Company pipeline yêu cầu qua Thử việc trước khi gửi Offer");
        }

        if (currentStage == ApplicationStage.OFFBOARDED && targetStage != ApplicationStage.OFFBOARDED) {
            throw new RuntimeException("Không thể chuyển stage sau khi hồ sơ đã ở trạng thái Nghỉ việc");
        }

        if (currentStage == ApplicationStage.HIRED && targetStage == ApplicationStage.OFFBOARDED) {
            return;
        }

        if (currentStage == ApplicationStage.OFFER_ACCEPTED && targetStage == ApplicationStage.HIRED) {
            return;
        }
    }

    private ApplicationStage resolveNextActiveStage(
            List<CompanyRecruitmentStage> stages,
            ApplicationStage currentStage) {
        if (stages == null || stages.isEmpty() || currentStage == null) {
            return null;
        }

        stages.sort(Comparator.comparingInt(stage -> stage.getDisplayOrder() == null ? 0 : stage.getDisplayOrder()));
        int currentIndex = -1;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).getStage() == currentStage) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            return null;
        }

        for (int i = currentIndex + 1; i < stages.size(); i++) {
            CompanyRecruitmentStage next = stages.get(i);
            if (next != null && next.isActive()) {
                return next.getStage();
            }
        }

        return null;
    }

    private static boolean isHrMessagingPromotion(ApplicationStage currentStage, ApplicationStage targetStage) {
        return targetStage == ApplicationStage.HR_CONTACTED
                && (currentStage == ApplicationStage.APPLIED || currentStage == ApplicationStage.SCREENING);
    }

    private static boolean isAiScreeningPromotion(ApplicationStage currentStage, ApplicationStage targetStage) {
        return currentStage == ApplicationStage.APPLIED && targetStage == ApplicationStage.SCREENING;
    }

    private void validateHrContactedStageActive(Company company, ApplicationStage targetStage) {
        if (company != null && ApplicationStage.isConfigurableStage(targetStage)) {
            boolean activeStage = companyRecruitmentStageService
                    .isStageActiveForCompany(company.getId(), targetStage);
            if (!activeStage) {
                throw new RuntimeException("Stage hiện đã bị tắt trong pipeline của công ty");
            }
        }
    }

    private void validateAiRejectedRecovery(
            Application application,
            Company company,
            ApplicationStage targetStage,
            String applicationId) {
        if (!AI_REJECTED_RECOVERY_STAGES.contains(targetStage) || !isLatestRejectionCreatedByAi(application)) {
            throw new RuntimeException(String.format(
                    "Stage transition from REJECTED to %s is not allowed for application %s",
                    targetStage, applicationId));
        }

        if (company != null && ApplicationStage.isConfigurableStage(targetStage)) {
            boolean activeStage = companyRecruitmentStageService
                    .isStageActiveForCompany(company.getId(), targetStage);
            if (!activeStage) {
                throw new RuntimeException("Stage hiện đã bị tắt trong pipeline của công ty");
            }
        }
    }

    private boolean isLatestRejectionCreatedByAi(Application application) {
        if (application == null || application.getStageHistory() == null || application.getStageHistory().isEmpty()) {
            return false;
        }

        return application.getStageHistory().stream()
                .filter(history -> history != null && history.getToStage() == ApplicationStage.REJECTED)
                .max(Comparator.comparing(ApplicationStageHistory::getChangedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(history -> ApplicationAiScreeningServiceImpl.AI_SCREENING_ACTOR.equals(history.getChangedBy()))
                .orElse(false);
    }

    private static Map<ApplicationStage, Set<ApplicationStage>> buildBaseTransitions() {
        Map<ApplicationStage, Set<ApplicationStage>> transitions = new EnumMap<>(ApplicationStage.class);

        transitions.put(ApplicationStage.APPLIED,
                EnumSet.of(ApplicationStage.SCREENING, ApplicationStage.HR_CONTACTED, ApplicationStage.SCHEDULED,
                        ApplicationStage.INTERVIEW, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.SCREENING,
                EnumSet.of(ApplicationStage.HR_CONTACTED, ApplicationStage.SCHEDULED,
                        ApplicationStage.INTERVIEW, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.HR_CONTACTED,
                EnumSet.of(ApplicationStage.SCHEDULED, ApplicationStage.INTERVIEW,
                        ApplicationStage.INTERVIEW_SCHEDULED, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.SCHEDULED,
                EnumSet.of(ApplicationStage.INTERVIEW, ApplicationStage.INTERVIEW_SCHEDULED,
                        ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.INTERVIEW,
                EnumSet.of(ApplicationStage.INTERVIEW_SCHEDULED, ApplicationStage.INTERVIEW_COMPLETED,
                        ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.INTERVIEW_SCHEDULED,
                EnumSet.of(ApplicationStage.INTERVIEW, ApplicationStage.INTERVIEW_COMPLETED,
                        ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.INTERVIEW_COMPLETED,
                EnumSet.of(ApplicationStage.TRIAL, ApplicationStage.OFFER_EXTENDED,
                        ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN));
        transitions.put(ApplicationStage.TRIAL,
                EnumSet.of(ApplicationStage.HIRED, ApplicationStage.REJECTED,
                        ApplicationStage.WITHDRAWN, ApplicationStage.OFFER_EXTENDED,
                        ApplicationStage.OFFBOARDED));
        transitions.put(ApplicationStage.OFFER_EXTENDED,
                EnumSet.of(ApplicationStage.OFFER_ACCEPTED, ApplicationStage.OFFER_DECLINED,
                        ApplicationStage.HIRED, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN,
                        ApplicationStage.TRIAL));
        transitions.put(ApplicationStage.OFFER_ACCEPTED,
                EnumSet.of(ApplicationStage.HIRED, ApplicationStage.TRIAL));
        transitions.put(ApplicationStage.OFFER_DECLINED,
                EnumSet.noneOf(ApplicationStage.class));
        transitions.put(ApplicationStage.HIRED,
                EnumSet.of(ApplicationStage.OFFBOARDED));
        transitions.put(ApplicationStage.OFFBOARDED,
                EnumSet.noneOf(ApplicationStage.class));
        transitions.put(ApplicationStage.REJECTED,
                EnumSet.noneOf(ApplicationStage.class));
        transitions.put(ApplicationStage.WITHDRAWN,
                EnumSet.noneOf(ApplicationStage.class));

        return transitions;
    }

    private String resolveStageNote(ApplicationStage stage, String providedNote) {
        if (StringUtils.hasText(providedNote)) {
            return providedNote.trim();
        }

        return switch (stage) {
            case APPLIED -> SUBMISSION_NOTE;
            case SCHEDULED -> "Lịch phỏng vấn của bạn đã được tạo.";
            case SCREENING -> "Hồ sơ của bạn đang được HR sàng lọc.";
            case INTERVIEW -> "Bạn đã được chuyển sang giai đoạn phỏng vấn. Vui lòng kiểm tra lịch.";
            case HR_CONTACTED -> "HR sẽ liên hệ với bạn trong thời gian sớm nhất.";
            case INTERVIEW_SCHEDULED -> "Lịch phỏng vấn đã được sắp xếp.";
            case INTERVIEW_COMPLETED -> "Phỏng vấn đã hoàn tất và đang được đánh giá.";
            case TRIAL -> "Bạn đã bước vào giai đoạn thử việc.";
            case OFFER_EXTENDED -> "Nhà tuyển dụng đã gửi đề nghị làm việc.";
            case OFFER_ACCEPTED -> "Bạn đã chấp nhận đề nghị làm việc.";
            case OFFER_DECLINED -> "Bạn đã từ chối đề nghị làm việc.";
            case HIRED -> "Chào mừng bạn gia nhập công ty.";
            case OFFBOARDED -> "Hồ sơ đã được cập nhật sang trạng thái nghỉ việc.";
            case REJECTED -> "Chúng tôi rất tiếc, hồ sơ của bạn chưa phù hợp ở lần này.";
            case WITHDRAWN -> "Bạn đã rút hồ sơ khỏi quy trình tuyển dụng.";
        };
    }

    private void dispatchStageEmail(Application application, ApplicationStage stage, String note, String actor) {
        Candidate candidate = application.getCandidate();
        if (candidate == null) {
            log.warn("No candidate linked to application {}. Skipping stage email.", application.getId());
            return;
        }

        String recipientEmail = resolveCandidateEmail(candidate);
        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("No recipient email found for candidate {}. Stage email not sent.", candidate.getId());
            return;
        }

        String candidateName = resolveCandidateDisplayName(candidate);
        String jobTitle = Optional.ofNullable(application.getJob())
                .map(Job::getTitle)
                .filter(StringUtils::hasText)
                .orElse("your applied position");
        String companyName = Optional.ofNullable(application.getJob())
                .map(Job::getCompany)
                .map(Company::getTagname)
                .filter(StringUtils::hasText)
                .orElse("CareerGraph");

        try {
            // Each state change triggers a tailored candidate notification.
            mailService.sendApplicationStageUpdateEmail(recipientEmail, candidateName, jobTitle, companyName, stage,
                    note);
        } catch (Exception ex) {
            log.error("Failed to send stage update email for application {}", application.getId(), ex);
        }
    }

    private String resolveCandidateEmail(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate.getAccount() != null && StringUtils.hasText(candidate.getAccount().getEmail())) {
            return candidate.getAccount().getEmail();
        }
        if (candidate.getContacts() == null || candidate.getContacts().isEmpty()) {
            return null;
        }

        return candidate.getContacts().stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .sorted(Comparator.comparing(contact -> Boolean.TRUE.equals(contact.getIsPrimary()) ? 0 : 1))
                .map(Contact::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String resolveCandidateDisplayName(Candidate candidate) {
        if (candidate == null) {
            return "Candidate";
        }

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(candidate.getFirstName())) {
            builder.append(candidate.getFirstName().trim());
        }
        if (StringUtils.hasText(candidate.getLastName())) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(candidate.getLastName().trim());
        }

        if (!builder.isEmpty()) {
            return builder.toString();
        }

        if (StringUtils.hasText(candidate.getTagname())) {
            return candidate.getTagname().trim();
        }

        return "Candidate";
    }

    private String resolveActorLabel(String providedActor, Candidate candidate) {
        if (StringUtils.hasText(providedActor)) {
            return providedActor.trim();
        }
        return String.format("candidate:%s", Optional.ofNullable(candidate)
                .map(Candidate::getId)
                .orElse("unknown"));
    }
}
