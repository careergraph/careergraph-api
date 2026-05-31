package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class FileResponse{
        String id;
        String url;
        String publicId;
        String fileName;
        String originalFileName;
        String createdAt;
        String ownerType;
        String idd;
        String fileType;
        boolean shareToFindJob;
        boolean shareToFileJob;
}
