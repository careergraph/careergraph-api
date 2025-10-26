package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.models.Candidate;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateService {

    String updateResource(String candidateId, MultipartFile file, FileType fileType) throws ChangeSetPersister.NotFoundException;

    String getResource(String candidateId, FileType fileType) throws ChangeSetPersister.NotFoundException;

    Candidate getMyProfile(String candidateId) throws ChangeSetPersister.NotFoundException;

    Candidate updateInformation (String candidateId, CandidateRequest.UpdateInformation candidateRequest) throws ChangeSetPersister.NotFoundException;
}
