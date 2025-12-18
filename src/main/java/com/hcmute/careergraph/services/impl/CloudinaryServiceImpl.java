package com.hcmute.careergraph.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.mapper.CloudFileMapper;
import com.hcmute.careergraph.mapper.FileMapper;
import com.hcmute.careergraph.persistence.dtos.response.CloudFileResponse;
import com.hcmute.careergraph.persistence.dtos.response.FileResponse;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.AuthToken;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Cloudinary service implementation
 *
 * - Upload image / video / raw (PDF, DOCX...)
 * - ALWAYS use secure_url returned by Cloudinary
 * - Store resource_type to avoid wrong URL generation (401)
 */
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    /* ================= UPLOAD IMAGE ================= */

    @Override
    public String uploadImage(MultipartFile file, String ownerType, String idd, FileType fileType)
            throws IOException {
        return uploadInternal(file, ownerType, idd, fileType, "image").getUrl();
    }

    /* ================= UPLOAD VIDEO ================= */

    @Override
    public String uploadVideo(MultipartFile file, String ownerType, String idd, FileType fileType)
            throws IOException {
        return uploadInternal(file, ownerType, idd, fileType, "video").getUrl();
    }

    /* ================= UPLOAD FILE (PDF / DOCX) ================= */

    @Override
    public FileResponse uploadFile(MultipartFile file, String ownerType, String idd, FileType fileType)
            throws IOException {
        return uploadInternal(file, ownerType, idd, fileType, "raw");
    }

    /* ================= CORE ================= */

    private FileResponse uploadInternal(
            MultipartFile multipart,
            String ownerType,
            String idd,
            FileType fileType,
            String resourceType
    ) throws IOException {

        if (multipart == null || multipart.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (StringUtils.isAnyBlank(ownerType, idd) || fileType == null) {
            throw new IllegalArgumentException("Invalid params");
        }

        String folder = ownerType + "/" + idd + "/" + fileType.name();
        String original = Objects.requireNonNull(multipart.getOriginalFilename());
        String safeName = original.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        String publicId = UUID.randomUUID() + "_" + safeName;

        Path tmp = Files.createTempFile("upload_", "_" + safeName);
        multipart.transferTo(tmp);

        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", resourceType,   // image | video | raw
                    "folder", folder,
                    "public_id", publicId,
                    "overwrite", false,
                    "access_mode", "public"          // 🔥 BẮT BUỘC → KHÔNG 401
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result =
                    cloudinary.uploader().upload(tmp.toFile(), options);

            File entity = fileMapper.toFile(result);
            entity.setOwnerId(idd);
            entity.setOwnerType(PartyType.fromLabel(ownerType));
            entity.setFileType(fileType);

            fileRepository.save(entity);
            return fileMapper.toFileResponse(entity);

        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /* ================= DELETE ================= */

    @Override
    public boolean deleteByPublicId(String candidateId, String publicId) throws IOException {

        if (StringUtils.isAnyBlank(candidateId, publicId)) {
            throw new IllegalArgumentException("Invalid params");
        }

        if (!publicId.startsWith("candidate/" + candidateId + "/")) {
            throw new SecurityException("Not allowed");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> res =
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

        String r = Objects.toString(res.get("result"), "");
        return "ok".equalsIgnoreCase(r) || "deleted".equalsIgnoreCase(r);
    }

    /* ======================= LIST ======================= */

    @Override
    public List<CloudFileResponse> listFiles(
            String ownerType,
            String idd,
            FileType fileType
    ) throws IOException {

        if (StringUtils.isAnyBlank(ownerType, idd)) {
            throw new IllegalArgumentException("ownerType and idd are required");
        }

        String prefix = (fileType == null)
                ? ownerType + "/" + idd + "/"
                : ownerType + "/" + idd + "/" + fileType.name() + "/";

        Map<String, Object> params = ObjectUtils.asMap(
                "type", "upload",
                "prefix", prefix,
                "max_results", 500
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = cloudinary.api().resources(params);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resources =
                    (List<Map<String, Object>>) resp.get("resources");

            return CloudFileMapper.mapList(resources, ownerType, idd, fileType);

        } catch (Exception e) {
            throw new IOException("Failed to list Cloudinary resources", e);
        }
    }
    /* ======================= HELPERS ======================= */

    private java.io.File convertToTempFile(MultipartFile file, String extension)
            throws IOException {

        String suffix = (extension == null || extension.isBlank())
                ? ""
                : "." + extension;

        Path tmp = Files.createTempFile("upload_", suffix);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp.toFile();
    }

    private void cleanDisk(java.io.File file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }

    private List<String> buildTags(String ownerType, String idd, FileType fileType) {
        return List.of(
                "owner:" + idd,
                "ownerType:" + ownerType,
                "type:" + fileType.name()
        );
    }

    private String sanitizeFilename(String s) {
        return (s == null) ? "file" : s.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    }

    private String getBaseName(String originalName) {
        int idx = originalName.lastIndexOf('.');
        return (idx == -1) ? originalName : originalName.substring(0, idx);
    }

    private String getExtension(String originalName) {
        int idx = originalName.lastIndexOf('.');
        return (idx == -1) ? "" : originalName.substring(idx + 1);
    }



}
