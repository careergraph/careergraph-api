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
import com.hcmute.careergraph.persistence.event.ResumeFilePersistedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.*;
import java.util.Locale;

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
    private final ApplicationEventPublisher applicationEventPublisher;

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
        String original = normalizeUtf8Filename(Objects.requireNonNull(multipart.getOriginalFilename()));
        String displayName = buildDisplayName(original);
        String publicId = UUID.randomUUID().toString();

        Path inputTmp = Files.createTempFile("upload_", "_" + publicId);
        try (InputStream is = multipart.getInputStream()) {
            Files.copy(is, inputTmp, StandardCopyOption.REPLACE_EXISTING);
        }

        boolean convertToPdf = isWordDocument(original, multipart.getContentType());
        Path uploadPath = convertToPdf
                ? convertWordToPdf(inputTmp)
                : inputTmp;
        String uploadDisplayName = convertToPdf ? replaceExtension(displayName, "pdf") : displayName;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> options = (Map<String, Object>) ObjectUtils.asMap(
                    "resource_type", resourceType,   // image | video | raw
                    "folder", folder,
                    "public_id", publicId,
                    "overwrite", false,
                    "access_mode", "public",         // 🔥 BẮT BUỘC → KHÔNG 401
                    "display_name", uploadDisplayName,
                    "filename_override", uploadDisplayName,
                    "use_filename", true,
                    "unique_filename", false
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result =
                    cloudinary.uploader().upload(uploadPath.toFile(), options);

            File entity = fileMapper.toFile(result);
            entity.setOwnerId(idd);
            entity.setOwnerType(PartyType.fromLabel(ownerType));
            entity.setFileType(fileType);
            entity.setFileName(uploadDisplayName);
            entity.setOriginalFileName(original);
            if (convertToPdf) {
                entity.setMimeType("application/pdf");
            }

            fileRepository.save(entity);
            if (fileType == FileType.RESUME || fileType == FileType.CV) {
                applicationEventPublisher.publishEvent(new ResumeFilePersistedEvent(entity.getId()));
            }
            return fileMapper.toFileResponse(entity);

        } finally {
            Files.deleteIfExists(uploadPath);
            if (convertToPdf) {
                Files.deleteIfExists(uploadPath.getParent());
            }
            if (!Objects.equals(uploadPath, inputTmp)) {
                Files.deleteIfExists(inputTmp);
            }
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

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) ObjectUtils.asMap(
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

    private Path convertWordToPdf(Path inputFile) throws IOException {
        Path outDir = Files.createTempDirectory("cv_pdf_");
        List<String> commands = List.of("soffice", "libreoffice");

        IOException lastError = null;
        for (String command : commands) {
            try {
                Process process = new ProcessBuilder(
                        command,
                        "--headless",
                        "--nologo",
                        "--nofirststartwizard",
                        "--convert-to",
                        "pdf",
                        "--outdir",
                        outDir.toString(),
                        inputFile.toString()
                ).redirectErrorStream(true).start();

                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    lastError = new IOException("LibreOffice conversion failed: " + output.trim());
                    continue;
                }

                String inputBase = getBaseName(inputFile.getFileName().toString());
                Path converted = outDir.resolve(inputBase + ".pdf");
                if (Files.exists(converted)) {
                    return converted;
                }

                try (var stream = Files.list(outDir)) {
                    Optional<Path> firstPdf = stream
                            .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                            .findFirst();
                    if (firstPdf.isPresent()) {
                        return firstPdf.get();
                    }
                }

                lastError = new IOException("LibreOffice did not produce a PDF file.");
            } catch (IOException e) {
                lastError = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("LibreOffice conversion interrupted", e);
            }
        }

        throw new IOException("DOC/DOCX to PDF conversion requires LibreOffice (soffice/libreoffice) installed.", lastError);
    }

    private String normalizeUtf8Filename(String filename) {
        if (filename == null) {
            return "file";
        }

        return Normalizer.normalize(filename, Normalizer.Form.NFC)
                .replaceAll("[\\\\/:\u0000-\u001F]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isWordDocument(String filename, String contentType) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return lower.endsWith(".doc") || lower.endsWith(".docx")
                || "application/msword".equals(mime)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mime);
    }

    private String buildDisplayName(String originalFilename) {
        String baseName = getBaseName(originalFilename);
        String extension = getExtension(originalFilename);
        if (StringUtils.isBlank(extension)) {
            return originalFilename;
        }
        return baseName + "." + extension.toLowerCase(Locale.ROOT);
    }

    private String replaceExtension(String filename, String newExtension) {
        String baseName = getBaseName(filename);
        return baseName + "." + newExtension.toLowerCase(Locale.ROOT);
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
