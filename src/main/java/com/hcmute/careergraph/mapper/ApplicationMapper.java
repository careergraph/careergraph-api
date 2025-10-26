package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ApplicationResponse;
import com.hcmute.careergraph.persistence.models.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationMapper {

    private final CandidateMapper candidateMapper;
    private final JobMapper jobMapper;

    public ApplicationResponse toResponse(Application application) {
        if (application == null) {
            return null;
        }

        return ApplicationResponse.builder()
                .applicationId(application.getId())
                .coverLetter(application.getCoverLetter())
                .resumeUrl(application.getResumeUrl())
                .rating(application.getRating())
                .notes(application.getNotes())
                .appliedDate(application.getAppliedDate())
                .status(application.getStatus())
                .candidate(candidateMapper.toResponse(application.getCandidate()))
                .job(jobMapper.toResponse(application.getJob()))
                .build();
    }

    public List<ApplicationResponse> toResponseList(List<Application> applications) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyList();
        }

        return applications.stream()
                .map(this::toResponse)
                .toList();
    }
}
