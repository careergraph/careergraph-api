package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface S3StorageService {

    StoredFile uploadCandidateFile(String candidateId, FileType fileType, MultipartFile file) throws IOException;

    String getFileUrl(String objectKey);

    List<String> listCandidateFiles(String candidateId, FileType fileType);

    void deleteFile(String objectKey);

    void deleteFiles(Collection<String> objectKeys);

    record StoredFile(String key, String url) { }
}
