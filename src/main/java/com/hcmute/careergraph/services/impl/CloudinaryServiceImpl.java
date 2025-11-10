package com.hcmute.careergraph.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.mapper.CloudFileMapper;
import com.hcmute.careergraph.persistence.dtos.response.CloudFileResponse;
import com.hcmute.careergraph.services.CloudinaryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Cloudinary service implementation updated to use CloudFileMapper and CloudFileResponse.
 *
 * - Method parameter name for owner id is "idd" (as requested).
 * - listFiles(...) now returns mapped CloudFileResponse objects using CloudFileMapper.
 * - upload methods still return secure URL, and store files under folder: {ownerType}/{idd}/{fileType}/
 */
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile file, String ownerType, String idd, FileType fileType) throws IOException {
        return upload(file, ownerType, idd, fileType, "image", false);
    }

    @Override
    public String uploadVideo(MultipartFile file, String ownerType, String idd, FileType fileType) throws IOException {
        return upload(file, ownerType, idd, fileType, "video", true);
    }

    @Override
    public String uploadFile(MultipartFile file, String ownerType, String idd, FileType fileType) throws IOException {
        return upload(file, ownerType, idd, fileType, "auto", false);
    }

    private String upload(MultipartFile file,
                          String ownerType,
                          String idd,
                          FileType fileType,
                          String resourceType,
                          boolean useResourceType) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IOException("File is null or empty");
        }
        if (StringUtils.isAnyBlank(ownerType, idd) || fileType == null) {
            throw new IllegalArgumentException("ownerType, idd and fileType are required");
        }

        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;

        String folder = StringUtils.join(ownerType, "/", idd, "/", fileType.name());
        String safeName = sanitizeFilename(getBaseName(originalFilename));
        String publicIdBase = StringUtils.join(UUID.randomUUID().toString(), "_", safeName);
        File uploadFile = convertToTempFile(file, getExtension(originalFilename));

        Map<String, Object> uploadOptions = ObjectUtils.asMap(
                "folder", folder,
                "public_id", publicIdBase,
                "resource_type", resourceType,
                "tags", String.join(",", buildTags(ownerType, idd, fileType))
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(uploadFile, uploadOptions);
            String secureUrl = Objects.toString(result.get("secure_url"), null);
            if (secureUrl != null) {
                return secureUrl;
            }
            // fallback: construct url from public id
            String fullPublicId = folder + "/" + publicIdBase;
            if (useResourceType) {
                return cloudinary.url().resourceType(resourceType).generate(fullPublicId);
            } else {
                return cloudinary.url().generate(fullPublicId);
            }
        } finally {
            cleanDisk(uploadFile);
        }
    }

    @Override
    public List<CloudFileResponse> listFiles(String ownerType, String idd, FileType fileType) throws IOException {
        if (StringUtils.isAnyBlank(ownerType, idd)) {
            throw new IllegalArgumentException("ownerType and idd are required");
        }

        String prefix = (fileType == null || fileType.name().isBlank())
                ? StringUtils.join(ownerType, "/", idd, "/")
                : StringUtils.join(ownerType, "/", idd, "/", fileType, "/");

        Map<String, Object> params = ObjectUtils.asMap(
                "type", "upload",
                "prefix", prefix,
                "max_results", 500
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = cloudinary.api().resources(params);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resources = (List<Map<String, Object>>) resp.get("resources");
            // Map Cloudinary resource maps into CloudFileResponse using CloudFileMapper
            return CloudFileMapper.mapList(resources, ownerType, idd, fileType);
        } catch (Exception e) {
            throw new IOException("Failed to list Cloudinary resources: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByPublicId(String publicId) throws IOException {
        if (StringUtils.isBlank(publicId)) {
            throw new IllegalArgumentException("publicId is required");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String result = Objects.toString(res.get("result"), "");
            return "ok".equalsIgnoreCase(result) || "deleted".equalsIgnoreCase(result);
        } catch (Exception e) {
            throw new IOException("Failed to delete resource: " + e.getMessage(), e);
        }
    }

    /* ------------------- helpers ------------------- */

    private File convertToTempFile(MultipartFile file, String extension) throws IOException {
        String suffix = (extension == null || extension.isBlank()) ? "" : "." + extension;
        Path tmp = Files.createTempFile("upload_", suffix);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp.toFile();
    }

    private void cleanDisk(File file) {
        if (file == null) return;
        try {
            Path filePath = file.toPath();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete temp file: " + e.getMessage());
        }
    }

    private List<String> buildTags(String ownerType, String idd, FileType fileType) {
        String typeName = (fileType == null) ? "" : fileType.name();
        return Arrays.asList("owner:" + idd, "type:" + typeName, "ownerType:" + ownerType);
    }

    private String sanitizeFilename(String s) {
        if (s == null) return "file";
        return s.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    }

    private String getBaseName(String originalName) {
        if (originalName == null) return "file";
        int idx = originalName.lastIndexOf('.');
        return (idx == -1) ? originalName : originalName.substring(0, idx);
    }

    private String getExtension(String originalName) {
        if (originalName == null) return "";
        int idx = originalName.lastIndexOf('.');
        return (idx == -1) ? "" : originalName.substring(idx + 1);
    }
}