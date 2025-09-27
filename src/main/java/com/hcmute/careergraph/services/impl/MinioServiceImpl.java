package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.services.MinioService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${integration.minio.bucket}")
    private String bucketName;

    /*
    * Init constructor
    * */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient
                    .bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            log.error("MinIO error: ", e.getMessage());
            throw new InternalError("Cannot initialize MinIO bucket: ", e);
        }
    }

    @Override
    public String uploadFile(String objectName, MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new InternalError("Error uploading file to MinIO: ", e);
        }
    }

    @Override
    public String getFileUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(60 * 60) // 1h
                    .build()
            );
        } catch (Exception e) {
            throw new InternalError("Error generating file URL", e);
        }
    }
}
