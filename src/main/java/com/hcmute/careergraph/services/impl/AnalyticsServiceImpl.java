package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.interview.InterviewStatus;
import com.hcmute.careergraph.persistence.dtos.response.DashboardSummaryResponse;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.ApplicationStageHistory;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.ApplicationStageHistoryRepository;
import com.hcmute.careergraph.repositories.InterviewRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

  private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy",
      Locale.ENGLISH);

  private static final List<InterviewStatus> SCHEDULED_INTERVIEW_STATUSES = List.of(
      InterviewStatus.SCHEDULED,
      InterviewStatus.CONFIRMED,
      InterviewStatus.PENDING_RESCHEDULE,
      InterviewStatus.IN_PROGRESS);

  private static final Collection<Status> TARGET_JOB_STATUSES = EnumSet.of(
      Status.ACTIVE,
      Status.OPEN,
      Status.DRAFT,
      Status.PAUSED);

  private final ApplicationRepository applicationRepository;
  private final ApplicationStageHistoryRepository applicationStageHistoryRepository;
  private final InterviewRepository interviewRepository;
  private final JobRepository jobRepository;

  @Override
  public DashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to, String companyId) {
    DateWindow window = DateWindow.of(from, to);
    DateWindow previousWindow = window.previousWindow();

    List<ApplicationSnapshot> snapshots = loadApplicationSnapshots(companyId);

    long candidatesCurrent = countUniqueCandidatesByAppliedDate(snapshots, window);
    long candidatesPrevious = countUniqueCandidatesByAppliedDate(snapshots, previousWindow);

    long newApplicationsCurrent = countApplicationsByAppliedDate(snapshots, window);
    long newApplicationsPrevious = countApplicationsByAppliedDate(snapshots, previousWindow);

    long scheduledInterviewsCurrent = interviewRepository.countByCompanyIdAndInterviewStatusInAndScheduledAtBetween(
        companyId,
        SCHEDULED_INTERVIEW_STATUSES,
        window.fromDateTime(),
        window.toDateTime());

    long scheduledInterviewsPrevious = interviewRepository.countByCompanyIdAndInterviewStatusInAndScheduledAtBetween(
        companyId,
        SCHEDULED_INTERVIEW_STATUSES,
        previousWindow.fromDateTime(),
        previousWindow.toDateTime());

    List<ApplicationStageHistory> stageHistories = applicationStageHistoryRepository
        .findByApplicationJobCompanyIdAndChangedAtBetween(
            companyId,
            window.fromDateTime(),
            window.toDateTime());

    DashboardSummaryResponse.KpiSummary kpiSummary = DashboardSummaryResponse.KpiSummary.builder()
        .candidates(toMetricValue(candidatesCurrent, candidatesPrevious))
        .newApplications(toMetricValue(newApplicationsCurrent, newApplicationsPrevious))
        .scheduledInterviews(toMetricValue(scheduledInterviewsCurrent, scheduledInterviewsPrevious))
        .build();

    DashboardSummaryResponse.PipelineVelocity pipelineVelocity = DashboardSummaryResponse.PipelineVelocity.builder()
        .monthly(buildPipelineVelocity(stageHistories, window))
        .build();

    DashboardSummaryResponse.FunnelConversion funnelConversion = DashboardSummaryResponse.FunnelConversion.builder()
        .monthly(buildFunnelConversion(stageHistories, window))
        .build();

    DashboardSummaryResponse.HiringTargetProgress hiringTargetProgress = buildHiringTargetProgress(
        companyId,
        window,
        previousWindow);

    List<DashboardSummaryResponse.RecentActivity> recentActivities = buildRecentActivities(companyId, window);

    return DashboardSummaryResponse.builder()
        .from(window.from().toString())
        .to(window.to().toString())
        .kpi(kpiSummary)
        .pipelineVelocity(pipelineVelocity)
        .hiringTargetProgress(hiringTargetProgress)
        .funnelConversion(funnelConversion)
        .recentActivities(recentActivities)
        .build();
  }

  private List<ApplicationSnapshot> loadApplicationSnapshots(String companyId) {
    return applicationRepository.findByJobCompanyId(companyId)
        .stream()
        .map(this::toSnapshot)
        .filter(Objects::nonNull)
        .toList();
  }

  private ApplicationSnapshot toSnapshot(Application application) {
    LocalDateTime appliedAt = parseAppliedAt(application.getAppliedDate());
    if (appliedAt == null) {
      return null;
    }

    String candidateId = application.getCandidate() != null ? application.getCandidate().getId() : null;
    return new ApplicationSnapshot(candidateId, appliedAt);
  }

  private long countApplicationsByAppliedDate(List<ApplicationSnapshot> snapshots, DateWindow window) {
    return snapshots.stream()
        .filter(snapshot -> window.contains(snapshot.appliedAt()))
        .count();
  }

  private long countUniqueCandidatesByAppliedDate(List<ApplicationSnapshot> snapshots, DateWindow window) {
    return snapshots.stream()
        .filter(snapshot -> window.contains(snapshot.appliedAt()))
        .map(ApplicationSnapshot::candidateId)
        .filter(StringUtils::hasText)
        .distinct()
        .count();
  }

  private DashboardSummaryResponse.MetricValue toMetricValue(long current, long previous) {
    return DashboardSummaryResponse.MetricValue.builder()
        .value(current)
        .changePercent(calculateChangePercent(current, previous))
        .build();
  }

  private List<DashboardSummaryResponse.MonthlyValue> buildPipelineVelocity(
      List<ApplicationStageHistory> stageHistories,
      DateWindow window) {
    Map<YearMonth, Long> transitionByMonth = createMonthZeroMap(window);

    for (ApplicationStageHistory history : stageHistories) {
      if (history.getChangedAt() == null || history.getFromStage() == null) {
        continue;
      }
      YearMonth yearMonth = YearMonth.from(history.getChangedAt());
      transitionByMonth.computeIfPresent(yearMonth, (ignored, current) -> current + 1);
    }

    return transitionByMonth.entrySet()
        .stream()
        .map(entry -> DashboardSummaryResponse.MonthlyValue.builder()
            .monthLabel(entry.getKey().atDay(1).format(MONTH_LABEL_FORMATTER))
            .totalTransitions(entry.getValue())
            .build())
        .toList();
  }

  private List<DashboardSummaryResponse.MonthlyFunnelValue> buildFunnelConversion(
      List<ApplicationStageHistory> stageHistories,
      DateWindow window) {
    Map<YearMonth, Long> interviewCompletedByMonth = createMonthZeroMap(window);
    Map<YearMonth, Long> offersSentByMonth = createMonthZeroMap(window);

    for (ApplicationStageHistory history : stageHistories) {
      if (history.getChangedAt() == null || history.getToStage() == null) {
        continue;
      }

      YearMonth yearMonth = YearMonth.from(history.getChangedAt());
      if (history.getToStage() == ApplicationStage.INTERVIEW_COMPLETED) {
        interviewCompletedByMonth.computeIfPresent(yearMonth, (ignored, current) -> current + 1);
      }
      if (history.getToStage() == ApplicationStage.OFFER_EXTENDED) {
        offersSentByMonth.computeIfPresent(yearMonth, (ignored, current) -> current + 1);
      }
    }

    List<DashboardSummaryResponse.MonthlyFunnelValue> result = new ArrayList<>();
    for (YearMonth yearMonth : interviewCompletedByMonth.keySet()) {
      result.add(DashboardSummaryResponse.MonthlyFunnelValue.builder()
          .monthLabel(yearMonth.atDay(1).format(MONTH_LABEL_FORMATTER))
          .interviewsCompleted(interviewCompletedByMonth.getOrDefault(yearMonth, 0L))
          .offersSent(offersSentByMonth.getOrDefault(yearMonth, 0L))
          .build());
    }

    return result;
  }

  private DashboardSummaryResponse.HiringTargetProgress buildHiringTargetProgress(
      String companyId,
      DateWindow currentWindow,
      DateWindow previousWindow) {
    LocalDate now = LocalDate.now();
    LocalDate quarterStart = startOfQuarter(now);

    LocalDateTime quarterStartDateTime = quarterStart.atStartOfDay();
    LocalDateTime quarterToNowDateTime = LocalDateTime.now();

    long quarterTargetPositions = safeLong(
        jobRepository.sumNumberOfPositionsByCompanyIdAndCreatedDateBetweenAndStatusIn(
            companyId,
            quarterStartDateTime,
            quarterToNowDateTime,
            TARGET_JOB_STATUSES));

    long hiredThisQuarter = applicationStageHistoryRepository
        .countByApplicationJobCompanyIdAndToStageAndChangedAtBetween(
            companyId,
            ApplicationStage.HIRED,
            quarterStartDateTime,
            quarterToNowDateTime);

    long hiredCurrentWindow = applicationStageHistoryRepository
        .countByApplicationJobCompanyIdAndToStageAndChangedAtBetween(
            companyId,
            ApplicationStage.HIRED,
            currentWindow.fromDateTime(),
            currentWindow.toDateTime());

    long hiredPreviousWindow = applicationStageHistoryRepository
        .countByApplicationJobCompanyIdAndToStageAndChangedAtBetween(
            companyId,
            ApplicationStage.HIRED,
            previousWindow.fromDateTime(),
            previousWindow.toDateTime());

    LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    long hiredThisWeek = applicationStageHistoryRepository.countByApplicationJobCompanyIdAndToStageAndChangedAtBetween(
        companyId,
        ApplicationStage.HIRED,
        weekStart.atStartOfDay(),
        LocalDateTime.now());

    long pendingOffers = applicationRepository.countByJobCompanyIdAndCurrentStageIn(
        companyId,
        EnumSet.of(ApplicationStage.OFFER_EXTENDED, ApplicationStage.OFFER_ACCEPTED));

    int completionPercent = quarterTargetPositions <= 0
        ? 0
        : (int) Math.min(100, Math.round((hiredThisQuarter * 100.0) / quarterTargetPositions));

    return DashboardSummaryResponse.HiringTargetProgress.builder()
        .completionPercent(completionPercent)
        .changePercent(calculateChangePercent(hiredCurrentWindow, hiredPreviousWindow))
        .quarterTargetPositions(quarterTargetPositions)
        .hiredThisWeek(hiredThisWeek)
        .pendingOffers(pendingOffers)
        .build();
  }

  private List<DashboardSummaryResponse.RecentActivity> buildRecentActivities(String companyId, DateWindow window) {
    List<ApplicationStageHistory> histories = applicationStageHistoryRepository
        .findTop20ByApplicationJobCompanyIdAndChangedAtBetweenOrderByChangedAtDesc(
            companyId,
            window.fromDateTime(),
            window.toDateTime());

    return histories.stream()
        .map(this::toRecentActivity)
        .filter(Objects::nonNull)
        .toList();
  }

  private DashboardSummaryResponse.RecentActivity toRecentActivity(ApplicationStageHistory history) {
    if (history.getApplication() == null || history.getApplication().getCandidate() == null
        || history.getApplication().getJob() == null) {
      return null;
    }

    String firstName = history.getApplication().getCandidate().getFirstName();
    String lastName = history.getApplication().getCandidate().getLastName();
    String candidateName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

    if (!StringUtils.hasText(candidateName)) {
      candidateName = "Unknown Candidate";
    }

    ApplicationStage stage = history.getToStage();
    String stageLabel = stage != null ? stage.getLabel() : "Unknown stage";

    return DashboardSummaryResponse.RecentActivity.builder()
        .applicationId(history.getApplication().getId())
        .candidateId(history.getApplication().getCandidate().getId())
        .candidateName(candidateName)
        .candidateAvatar(history.getApplication().getCandidate().getAvatar())
        .jobTitle(history.getApplication().getJob().getTitle())
        .stage(stageLabel)
        .updatedBy(StringUtils.hasText(history.getChangedBy()) ? history.getChangedBy() : "System")
        .statusTag(resolveStatusTag(stage))
        .updatedAt(history.getChangedAt())
        .build();
  }

  private String resolveStatusTag(ApplicationStage stage) {
    if (stage == null) {
      return "INTERVIEW";
    }

    if (stage == ApplicationStage.HIRED) {
      return "HIRED";
    }

    if (stage == ApplicationStage.REJECTED || stage == ApplicationStage.WITHDRAWN
        || stage == ApplicationStage.OFFER_DECLINED) {
      return "REJECTED";
    }

    if (stage == ApplicationStage.OFFER_EXTENDED || stage == ApplicationStage.OFFER_ACCEPTED) {
      return "OFFERED";
    }

    return "INTERVIEW";
  }

  private Map<YearMonth, Long> createMonthZeroMap(DateWindow window) {
    Map<YearMonth, Long> values = new LinkedHashMap<>();
    YearMonth cursor = YearMonth.from(window.from());
    YearMonth end = YearMonth.from(window.to());

    while (!cursor.isAfter(end)) {
      values.put(cursor, 0L);
      cursor = cursor.plusMonths(1);
    }

    return values;
  }

  private LocalDate startOfQuarter(LocalDate date) {
    int startMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
    return LocalDate.of(date.getYear(), startMonth, 1);
  }

  private double calculateChangePercent(long current, long previous) {
    if (previous == 0) {
      return current == 0 ? 0.0 : 100.0;
    }

    double change = ((current - previous) * 100.0) / previous;
    return Math.round(change * 10.0) / 10.0;
  }

  private LocalDateTime parseAppliedAt(String rawAppliedDate) {
    if (!StringUtils.hasText(rawAppliedDate)) {
      return null;
    }

    String normalized = rawAppliedDate.trim();

    if (!normalized.contains("T") && normalized.contains(" ")) {
      normalized = normalized.replace(" ", "T");
    }

    List<DateTimeFormatter> formatters = List.of(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

    for (DateTimeFormatter formatter : formatters) {
      try {
        if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
          return OffsetDateTime.parse(normalized, formatter).toLocalDateTime();
        }
        if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
          return LocalDate.parse(normalized, formatter).atStartOfDay();
        }
        return LocalDateTime.parse(normalized, formatter);
      } catch (DateTimeParseException ignored) {
        // Continue with next parser.
      }
    }

    try {
      return Instant.parse(normalized).atZone(ZoneId.systemDefault()).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private long safeLong(Long value) {
    return value == null ? 0L : value;
  }

  private record ApplicationSnapshot(String candidateId, LocalDateTime appliedAt) {
  }

  private record DateWindow(LocalDate from, LocalDate to) {

    static DateWindow of(LocalDate from, LocalDate to) {
      LocalDate resolvedTo = to != null ? to : LocalDate.now();
      LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(29);

      if (resolvedFrom.isAfter(resolvedTo)) {
        throw new IllegalArgumentException("From date must not be after to date");
      }

      return new DateWindow(resolvedFrom, resolvedTo);
    }

    DateWindow previousWindow() {
      long days = daysInclusive();
      LocalDate previousTo = from.minusDays(1);
      LocalDate previousFrom = previousTo.minusDays(days - 1);
      return new DateWindow(previousFrom, previousTo);
    }

    long daysInclusive() {
      return from.datesUntil(to.plusDays(1)).count();
    }

    LocalDateTime fromDateTime() {
      return from.atStartOfDay();
    }

    LocalDateTime toDateTime() {
      return to.plusDays(1).atStartOfDay().minusNanos(1);
    }

    boolean contains(LocalDateTime value) {
      if (value == null) {
        return false;
      }
      return !value.isBefore(fromDateTime()) && !value.isAfter(toDateTime());
    }
  }
}
