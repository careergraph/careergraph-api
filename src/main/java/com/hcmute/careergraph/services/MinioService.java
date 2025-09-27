package com.hcmute.careergraph.services;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    // Upload file
    String uploadFile(String objectName, MultipartFile file);

    // Get URL
    String getFileUrl(String objectName);
}
