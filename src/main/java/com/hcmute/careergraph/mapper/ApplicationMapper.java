package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ApplicationResponse;
import com.hcmute.careergraph.persistence.dtos.response.ApplicationStageHistoryResponse;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.ApplicationStageHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApplicationMapper {

    private final CandidateMapper candidateMapper;
    private final JobMapper jobMapper;

    public ApplicationResponse toResponse(Application application, boolean isDetail) {
        if (application == null) {
            return null;
        }

        ApplicationResponse applicationResponse = ApplicationResponse.builder()
                .applicationId(application.getId())
                .coverLetter(application.getCoverLetter())
                .resumeUrl(application.getResumeUrl())
                .rating(application.getRating())
                .notes(application.getNotes())
                .appliedDate(application.getAppliedDate())
                .currentStage(application.getCurrentStage())
                .stageChangedAt(application.getStageChangedAt())
                .stageNote(application.getCurrentStageNote())
                .stageHistory(mapStageHistory(application.getStageHistory()))
                .build();

        if (isDetail) {
            applicationResponse.setCandidate(candidateMapper.toResponse(application.getCandidate()));
            applicationResponse.setJob(jobMapper.toResponse(application.getJob()));
        }
        return applicationResponse;
    }

    public List<ApplicationResponse> toResponseList(List<Application> applications, boolean isDetail) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyList();
        }

        return applications.stream()
                .map(application -> this.toResponse(application, isDetail))
                .toList();
    }

    private List<ApplicationStageHistoryResponse> mapStageHistory(List<ApplicationStageHistory> historyEntries) {
        if (historyEntries == null || historyEntries.isEmpty()) {
            return Collections.emptyList();
        }

        return historyEntries.stream()
                .filter(Objects::nonNull)
                .map(entry -> ApplicationStageHistoryResponse.builder()
                        .fromStage(entry.getFromStage())
                        .toStage(entry.getToStage())
                        .note(entry.getNote())
                        .changedBy(entry.getChangedBy())
                        .changedAt(entry.getChangedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
