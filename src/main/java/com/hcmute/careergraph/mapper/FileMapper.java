package com.hcmute.careergraph.mapper;

import com.cloudinary.utils.StringUtils;
import com.hcmute.careergraph.persistence.dtos.response.FileResponse;
import com.hcmute.careergraph.persistence.models.File;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Component
public class FileMapper {

    public File toFile(Map<String, Object> result) {
        if (result == null || result.isEmpty()) return null;

        File f = new File();

        // public_id
        String publicId = Objects.toString(result.get("public_id"), null);
        f.setPublicId(publicId);

        // filePath = secure_url (ưu tiên) hoặc url thường
        String secureUrl = Objects.toString(result.get("secure_url"), null);
        String url = Objects.toString(result.get("url"), null);
        f.setFilePath(secureUrl != null ? secureUrl : url);

        // fileName: ưu tiên display_name → original_filename → đoạn cuối của public_id
        String displayName = Objects.toString(result.get("display_name"), null);
        String originalFilename = Objects.toString(result.get("original_filename"), null);

        String fileName = displayName;
        if (StringUtils.isBlank(fileName)) {
            fileName = originalFilename;
        }
        if (StringUtils.isBlank(fileName) && publicId != null) {
            // lấy phần sau cùng
            String[] parts = publicId.split("/");
            fileName = parts[parts.length - 1];
        }

        f.setFileName(fileName);
        f.setOriginalFileName(originalFilename);

        // mime / format / resource_type
        String format = Objects.toString(result.get("format"), null);         // pdf, jpg, png...
        String resourceType = Objects.toString(result.get("resource_type"), null); // image, video, raw...

        f.setResourceType(resourceType);

        // Nếu bạn muốn map mimeType sơ sơ:
        if (format != null) {
            switch (format.toLowerCase()) {
                case "pdf" -> f.setMimeType("application/pdf");
                case "doc" -> f.setMimeType("application/msword");
                case "docx" ->
                        f.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                default -> f.setMimeType(null); // hoặc đoán theo format khác
            }
        }

        // size (bytes)
        Object bytesObj = result.get("bytes");
        if (bytesObj instanceof Number n) {
            f.setSizeBytes(n.longValue());
        }

        // uploadedAt
        Object createdAt = result.get("created_at");
        if (createdAt != null) {
            try {
                Instant instant = Instant.parse(createdAt.toString());
                LocalDateTime createdDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                f.setCreatedDate(createdDateTime);
            } catch (Exception e) {
                System.err.println("Parse created_at failed: " + e.getMessage());
            }
        }

        // thumbnail: nếu resource_type = image, có thể tự generate sau, tạm để null
        f.setThumbnail(null);

        // shareToFindJob, status sẽ dùng default trong entity (false, ACTIVE)
        return f;
    }
    public FileResponse toFileResponse(File file) {
        if (file == null) {
            return null;
        }

        return FileResponse.builder()
                // URL file trên Cloudinary (hoặc nơi lưu trữ)
                .url(file.getFilePath())

                // publicId dùng cho xoá Cloudinary
                .publicId(file.getPublicId())

                // thời điểm upload
                .createdAt(
                        file.getCreatedDate() != null
                                ? file.getCreatedDate().toString()
                                : null
                )

                // ownerType: enum → String
                .ownerType(
                        file.getOwnerType() != null
                                ? file.getOwnerType().name()
                                : null
                )

                // idd: chính là ownerId
                .idd(file.getOwnerId())

                // fileType: enum → String
                .fileType(
                        file.getFileType() != null
                                ? file.getFileType().name()
                                : null
                )

                // shareToFileJob: map từ shareToFindJob (null mặc định là false)
                .shareToFileJob(Boolean.TRUE.equals(file.getShareToFindJob()))

                .build();
    }
}
