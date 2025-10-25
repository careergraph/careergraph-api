package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateDto;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateService {

    String updateResource(String candidateId, MultipartFile file, FileType fileType) throws ChangeSetPersister.NotFoundException;

    String getResource(String candidateId, FileType fileType) throws ChangeSetPersister.NotFoundException;

    CandidateDto getMyProfile(String candidateId) throws ChangeSetPersister.NotFoundException;

    CandidateDto updateInformation (String candidateId, CandidateRequest.UpdateInformation candidateRequest) throws ChangeSetPersister.NotFoundException;
}
