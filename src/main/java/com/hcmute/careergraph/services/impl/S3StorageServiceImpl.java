package com.hcmute.careergraph.services.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hcmute.careergraph.config.properties.BackblazeProperties;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.services.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final AmazonS3 amazonS3;
    private final BackblazeProperties properties;

    @PostConstruct
    void ensureBucket() {
        if (!amazonS3.doesBucketExistV2(properties.getBucket())) {
            amazonS3.createBucket(properties.getBucket());
        }
    }

    @Override
    public StoredFile uploadCandidateFile(String candidateId, FileType fileType, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        String key = buildObjectKey(candidateId, fileType, file.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(properties.getBucket(), key, inputStream, metadata);
        }

        return new StoredFile(key, getFileUrl(key));
    }

    @Override
    public String getFileUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return amazonS3.getUrl(properties.getBucket(), objectKey).toExternalForm();
    }

    @Override
    public List<String> listCandidateFiles(String candidateId, FileType fileType) {
        String prefix = resolvePrefix(candidateId, fileType);
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(properties.getBucket())
                .withPrefix(prefix);

        List<String> keys = new ArrayList<>();
        ListObjectsV2Result result;
        do {
            result = amazonS3.listObjectsV2(request);
            for (S3ObjectSummary summary : result.getObjectSummaries()) {
                if (!summary.getKey().endsWith("/")) {
                    keys.add(summary.getKey());
                }
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return keys;
    }

    @Override
    public void deleteFile(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        amazonS3.deleteObject(properties.getBucket(), objectKey);
    }

    @Override
    public void deleteFiles(Collection<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        List<String> keys = objectKeys.stream()
                .filter(StringUtils::hasText)
                .toList();

        if (keys.isEmpty()) {
            return;
        }

        if (keys.size() == 1) {
            deleteFile(keys.getFirst());
            return;
        }

        DeleteObjectsRequest request = new DeleteObjectsRequest(properties.getBucket())
                .withKeys(keys.toArray(new String[0]));
        amazonS3.deleteObjects(request);
    }

    private String buildObjectKey(String candidateId, FileType fileType, String originalFilename) {
        String prefix = resolvePrefix(candidateId, fileType);
        String sanitizedName = sanitizeFileName(originalFilename);
        String extension = extractExtension(sanitizedName);
        String baseName = sanitizedName.substring(0, sanitizedName.length() - extension.length());

        return null;
    }

    private String resolvePrefix(String candidateId, FileType fileType) {
        String normalizedId = Objects.requireNonNull(candidateId, "candidateId must not be null");
        return null;
    }

    private String sanitizeFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "file";
        }

        String filename = originalFilename.replace("\\", "/");
        filename = filename.contains("/") ? filename.substring(filename.lastIndexOf('/') + 1) : filename;

        String name = filename.replaceAll("[^A-Za-z0-9._-]", "-");
        return StringUtils.hasText(name) ? name.toLowerCase(Locale.ROOT) : "file";
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot) : "";
    }
}
