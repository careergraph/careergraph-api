package com.hcmute.careergraph.mapper;

import com.cloudinary.utils.StringUtils;
import com.hcmute.careergraph.persistence.dtos.response.FileResponse;
import com.hcmute.careergraph.persistence.models.File;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FileMapper {

    /* ===== Cloudinary result → Entity ===== */
    public File toFile(Map<String, Object> result) {
        if (result == null || result.isEmpty()) return null;

        File f = new File();

        // public_id
        String publicId = Objects.toString(result.get("public_id"), null);
        f.setPublicId(publicId);

        // secure_url (DÙNG NGUYÊN, KHÔNG TỰ BUILD)
        String secureUrl = Objects.toString(result.get("secure_url"), null);
        f.setFilePath(secureUrl);

        // resource_type
        String resourceType = Objects.toString(result.get("resource_type"), null);
        f.setResourceType(resourceType);

        // file name
        String displayName = Objects.toString(result.get("display_name"), null);
        String originalFilename = Objects.toString(result.get("original_filename"), null);

        String fileName = !StringUtils.isBlank(displayName)
                ? displayName
                : originalFilename;

        if (StringUtils.isBlank(fileName) && publicId != null) {
            fileName = publicId.substring(publicId.lastIndexOf('/') + 1);
        }

        f.setFileName(fileName);
        f.setOriginalFileName(originalFilename);

        // mime type
        String format = Objects.toString(result.get("format"), null);
        if (format != null) {
            switch (format.toLowerCase()) {
                case "pdf" -> f.setMimeType("application/pdf");
                case "doc" -> f.setMimeType("application/msword");
                case "docx" ->
                        f.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                case "jpg", "jpeg" -> f.setMimeType("image/jpeg");
                case "png" -> f.setMimeType("image/png");
            }
        }

        // size
        Object bytes = result.get("bytes");
        if (bytes instanceof Number n) {
            f.setSizeBytes(n.longValue());
        }

        // created_at
        Object createdAt = result.get("created_at");
        if (createdAt != null) {
            try {
                Instant instant = Instant.parse(createdAt.toString());
                f.setCreatedDate(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
            } catch (Exception ignored) {}
        }

        f.setThumbnail(null);
        return f;
    }

    /* ===== Entity → Response ===== */
    public FileResponse toFileResponse(File file) {
        if (file == null) return null;

        return FileResponse.builder()
                .id(file.getId())
                .url(file.getFilePath())
                .publicId(file.getPublicId())
            .fileName(file.getFileName())
            .originalFileName(file.getOriginalFileName())
                .createdAt(file.getCreatedDate() != null
                        ? file.getCreatedDate().toString()
                        : null)
                .ownerType(file.getOwnerType() != null
                        ? file.getOwnerType().name()
                        : null)
                .idd(file.getOwnerId())
                .fileType(file.getFileType() != null
                        ? file.getFileType().name()
                        : null)
                .shareToFileJob(Boolean.TRUE.equals(file.getShareToFindJob()))
                .build();
    }

    public List<FileResponse> toFileResponses(List<File> files) {
        if (files == null || files.isEmpty()) return List.of();

        return files.stream()
                .sorted(Comparator.comparing(File::getCreatedDate).reversed())
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }
}
