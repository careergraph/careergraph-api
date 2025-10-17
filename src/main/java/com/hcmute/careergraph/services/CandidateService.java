package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateService {

    String updateResource(String candidateId, MultipartFile file, FileType fileType) throws ChangeSetPersister.NotFoundException;

    String getResource(String candidateId, FileType fileType) throws ChangeSetPersister.NotFoundException;
}
