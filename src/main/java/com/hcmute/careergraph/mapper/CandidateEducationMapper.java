package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateClientResponse;
import com.hcmute.careergraph.persistence.dtos.response.CandidateResponse;
import com.hcmute.careergraph.persistence.models.BaseEntity;
import com.hcmute.careergraph.persistence.models.CandidateEducation;
import com.hcmute.careergraph.persistence.models.CandidateExperience;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CandidateEducationMapper {
    public CandidateClientResponse.CandidateEducationResponse toResponse(CandidateEducation candidateEducation){
        if(candidateEducation == null) return CandidateClientResponse.CandidateEducationResponse.builder().build();
        return CandidateClientResponse.CandidateEducationResponse.builder()
                .major(candidateEducation.getMajor())
                .startDate(candidateEducation.getStartDate())
                .endDate(candidateEducation.getEndDate())
                .degreeTitle(candidateEducation.getDegreeTitle())
                .description(candidateEducation.getDescription())
                .build();
    }
    public List<CandidateClientResponse.CandidateEducationResponse> toResponses(Set<CandidateEducation> candidateEducations){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        if(candidateEducations == null) return List.of();
        return candidateEducations.stream()
                .filter(BaseEntity::isActive)
                .sorted(Comparator.comparing(
                        (CandidateEducation edu) -> Year.parse(edu.getEndDate(), formatter).toString()
                ).reversed())
                .map(this::toResponse).collect(Collectors.toList());
    }
    public CandidateEducation toEntity(CandidateRequest.CandidateEducationRequest candidateRequest){
        if(candidateRequest == null) return CandidateEducation.builder().build();
        return CandidateEducation.builder()
                .major(candidateRequest.major())
                .startDate(candidateRequest.startDate())
                .endDate(candidateRequest.endDate())
                .degreeTitle(candidateRequest.degreeTitle())
                .description(candidateRequest.description())
                .build();
    }

    public CandidateEducation toEntity(CandidateRequest.CandidateEducationRequest candidateRequest, CandidateEducation candidateEducation){
        if(candidateRequest == null) return candidateEducation;
        candidateEducation.setMajor(candidateRequest.major());
        candidateEducation.setStartDate(candidateRequest.startDate());
        candidateEducation.setEndDate(candidateRequest.endDate());
        candidateEducation.setDegreeTitle(candidateRequest.degreeTitle());
        candidateEducation.setDescription(candidateRequest.description());
        return candidateEducation;
    }
}
