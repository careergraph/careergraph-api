package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String from;
  private String to;
  private KpiSummary kpi;
  private PipelineVelocity pipelineVelocity;
  private HiringTargetProgress hiringTargetProgress;
  private FunnelConversion funnelConversion;
  private List<RecentActivity> recentActivities;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MetricValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long value;
    private double changePercent;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KpiSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private MetricValue candidates;
    private MetricValue newApplications;
    private MetricValue scheduledInterviews;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String monthLabel;
    private long totalTransitions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PipelineVelocity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<MonthlyValue> monthly;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HiringTargetProgress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int completionPercent;
    private double changePercent;
    private long quarterTargetPositions;
    private long hiredThisWeek;
    private long pendingOffers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyFunnelValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String monthLabel;
    private long interviewsCompleted;
    private long offersSent;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FunnelConversion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<MonthlyFunnelValue> monthly;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentActivity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String applicationId;
    private String candidateId;
    private String candidateName;
    private String candidateAvatar;
    private String jobTitle;
    private String stage;
    private String updatedBy;
    private String statusTag;
    private LocalDateTime updatedAt;
  }
}
