package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.CandidateSkillResponse;
import com.hcmute.careergraph.persistence.models.CandidateExperience;
import com.hcmute.careergraph.persistence.models.CandidateSkill;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CandidateSkillMapper {

    public CandidateSkillResponse toResponse(CandidateSkill candidateSkill) {
        if(candidateSkill == null) { CandidateSkillResponse.builder().build(); }

        assert candidateSkill != null;
        return CandidateSkillResponse.builder()
                .candidateId(candidateSkill.getCandidate().getId())
                .skillId(candidateSkill.getSkill().getId())
                .endorsedBy(candidateSkill.getEndorsedBy())
                .endorsementCount(candidateSkill.getEndorsementCount())
                .isVerified(candidateSkill.getIsVerified())
                .yearsOfExperience(candidateSkill.getYearsOfExperience())
                .proficiencyLevel(candidateSkill.getProficiencyLevel())
                .skillName(candidateSkill.getSkill().getName())
                .build();
    }
    public Set<CandidateSkillResponse> toResponses(Set<CandidateSkill> candidateSkills) {
        if(candidateSkills == null) { return Set.of(); }
        return candidateSkills.stream().map(this::toResponse).collect(Collectors.toSet());
    }
    public List<CandidateSkillResponse> toResponseList(Set<CandidateSkill> candidateSkills) {

        if (candidateSkills == null) {
            return List.of();
        }

        return candidateSkills.stream()
                .sorted(Comparator.comparing(CandidateSkill::getCreatedDate))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
