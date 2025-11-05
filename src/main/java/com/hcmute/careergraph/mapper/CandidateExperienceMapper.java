package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateClientResponse;
import com.hcmute.careergraph.persistence.models.BaseEntity;
import com.hcmute.careergraph.persistence.models.CandidateExperience;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CandidateExperienceMapper {
    public CandidateClientResponse.CandidateExperienceResponse toResponse(CandidateExperience candidateExperience) {
        if(candidateExperience == null) return CandidateClientResponse.CandidateExperienceResponse.builder().build();

        return CandidateClientResponse.CandidateExperienceResponse.builder()
                .id(candidateExperience.getId())
                .description(candidateExperience.getDescription())
                .endDate(candidateExperience.getEndDate())
                .startDate(candidateExperience.getStartDate())
                .isCurrent(candidateExperience.getIsCurrent())
                .companyName(candidateExperience.getCompany().getName())
                .companyId(candidateExperience.getCompany().getId())
                .jobTitle(candidateExperience.getJobTitle())
                .build();
    }
    public List<CandidateClientResponse.CandidateExperienceResponse> toResponses(Set<CandidateExperience> candidateExperiences) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        if(candidateExperiences == null) return List.of();
        return candidateExperiences.stream().filter(BaseEntity::isActive)
                        .sorted(Comparator.comparing(
                                (CandidateExperience exp) -> YearMonth.parse(exp.getStartDate(), formatter)
                        ).reversed())
                        .map(this::toResponse).collect(Collectors.toList());
    }

    public CandidateExperience toEntity(CandidateRequest.CandidateExperienceRequest candidateRequest) {
        if(candidateRequest == null) return CandidateExperience.builder().build();
        return CandidateExperience.builder()
                .description(candidateRequest.description())
                .endDate(candidateRequest.endDate())
                .startDate(candidateRequest.startDate())
                .isCurrent(candidateRequest.isCurrent())
                .jobTitle(candidateRequest.jobTitle())
                .build();

    }

    public CandidateExperience toUpdateEntity(CandidateRequest.CandidateExperienceRequest candidateRequest, CandidateExperience candidateExperience) {
        if(candidateRequest == null ) return CandidateExperience.builder().build();

        candidateExperience.setDescription(candidateRequest.description() != null ? candidateRequest.description() : candidateExperience.getDescription() );
        candidateExperience.setEndDate(candidateRequest.endDate() != null ? candidateRequest.endDate() : candidateExperience.getEndDate() );
        candidateExperience.setStartDate(candidateRequest.startDate() != null ? candidateRequest.startDate() : candidateExperience.getStartDate() );
        candidateExperience.setIsCurrent(candidateRequest.isCurrent() != null ? candidateRequest.isCurrent() : candidateExperience.getIsCurrent());
        candidateExperience.setJobTitle(candidateRequest.jobTitle() != null ? candidateRequest.jobTitle() : candidateExperience.getJobTitle());
        return candidateExperience;

    }
}
