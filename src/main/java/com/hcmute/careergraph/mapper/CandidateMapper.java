package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.CandidateResponse;
import com.hcmute.careergraph.persistence.models.Candidate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CandidateMapper {

    public CandidateResponse toResponse(Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        return CandidateResponse.builder()
                .candidateId(candidate.getId())
                .email(candidate.getAccount() != null ? candidate.getAccount().getEmail() : null)
                .firstName(candidate.getFirstName())
                .lastName(candidate.getLastName())
                .dateOfBirth(candidate.getDateOfBirth())
                .gender(candidate.getGender())
                .currentJobTitle(candidate.getCurrentJobTitle())
                .currentCompany(candidate.getCurrentCompany())
                .industry(candidate.getIndustry())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .workLocation(candidate.getWorkLocation())
                .isOpenToWork(candidate.getIsOpenToWork())
                .summary(candidate.getSummary())
                .resume(primaryResume(candidate.getResumes()))
                .tagname(candidate.getTagname())
                .avatar(candidate.getAvatar())
                .cover(candidate.getCover())
                .noOfFollowers(candidate.getNoOfFollowers())
                .noOfFollowing(candidate.getNoOfFollowing())
                .noOfConnections(candidate.getNoOfConnections())
                .build();
    }

    private String primaryResume(List<String> resumes) {
        if (resumes == null || resumes.isEmpty()) {
            return null;
        }
        return resumes.get(0);
    }
}
