package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidateService {

    String updateResource(String candidateId, MultipartFile file, FileType fileType) throws ChangeSetPersister.NotFoundException;

    String getResource(String candidateId, FileType fileType) throws ChangeSetPersister.NotFoundException;

    Candidate getMyProfile(String candidateId) throws ChangeSetPersister.NotFoundException;

    Candidate updateInformation (String candidateId, CandidateRequest.UpdateInformationRequest candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate updateJobFindCriteriaInfo (String candidateId, CandidateRequest.UpdateJobCriteriaRequest candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate updateGeneralInfo(String candidateId, CandidateRequest.UpdateGeneralInfo candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate addExperience(String candidateId,  CandidateRequest.CandidateExperienceRequest candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate updateExperience(String candidateId, String experienceId, CandidateRequest.CandidateExperienceRequest candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate deleteExperience(String candidateId, String experienceId) throws ChangeSetPersister.NotFoundException;

    Candidate addEducation(String candidateId,  CandidateRequest.CandidateEducationRequest candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate updateEducation(String candidateId, String educationId, CandidateRequest.CandidateEducationRequest candidateRequest) throws ChangeSetPersister.NotFoundException;

    Candidate deleteEducation(String candidateId, String educationId) throws ChangeSetPersister.NotFoundException;

    Candidate replaceSkillsForUser(String candidateId, CandidateRequest.ReplaceSkillsRequest request) throws ChangeSetPersister.NotFoundException;

}
