package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.persistence.dtos.response.CloudFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CloudinaryService {
    /**
     * Upload image (avatar/cover...) and attach metadata (ownerType, idd, fileType).
     * Returns the public secure URL from Cloudinary.
     *
     * Note: parameter name "idd" is used as you requested (instead of ownerId).
     */
    String uploadImage(MultipartFile file, String ownerType, String idd, FileType fileType) throws IOException;

    /**
     * Upload generic file (cv, document...) and attach metadata (ownerType, idd, fileType).
     */
    String uploadFile(MultipartFile file, String ownerType, String idd, FileType fileType) throws IOException;

    /**
     * Upload video (resource_type = "video"), with owner metadata.
     */
    String uploadVideo(MultipartFile file, String ownerType, String idd, FileType fileType) throws IOException;

    /**
     * List files uploaded for given owner (ownerType + idd). If fileType is null, returns all types.
     * Returns mapped CloudFileResponse objects.
     */
    List<CloudFileResponse> listFiles(String ownerType, String idd, FileType fileType) throws IOException;

    /**
     * Delete a file by its public_id (the full Cloudinary public id, e.g. "candidates/123/avatar/uuid_name").
     */
    boolean deleteByPublicId(String publicId) throws IOException;
}