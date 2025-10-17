package com.hcmute.careergraph.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.careergraph.services.CloudinaryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) throws IOException {

        assert file.getOriginalFilename() != null;
        String publicValue = generatePublicValue(file.getOriginalFilename());

        String extension = getFileName(file.getOriginalFilename())[1];
        File fileUpload = convert(file);

        cloudinary.uploader().upload(fileUpload, ObjectUtils.asMap("public_id", publicValue));
        cleanDisk(fileUpload);

        return  cloudinary.url().generate(StringUtils.join(publicValue, ".", extension));
    }

    @Override
    public String uploadVideo(MultipartFile file) throws IOException {
        // Kiểm tra file
        if (file == null || file.isEmpty()) {
            throw new IOException("File is null or empty");
        }

        assert file.getOriginalFilename() != null;
        String publicValue = generatePublicValue(file.getOriginalFilename());
        String extension = getFileName(file.getOriginalFilename())[1];

        // Chuyển MultipartFile thành File tạm thời
        File fileUpload = convert(file);

        try {
            // Upload video lên Cloudinary với resource_type là video
            cloudinary.uploader().upload(fileUpload,
                    ObjectUtils.asMap(
                            "public_id", publicValue,
                            "resource_type", "video"
                    ));

            // Tạo URL cho video
            return cloudinary.url()
                    .resourceType("video")
                    .generate(StringUtils.join(publicValue, ".", extension));
        } finally {
            cleanDisk(fileUpload);
        }
    }

    private File convert(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        File convFile = new File(StringUtils.join(generatePublicValue(file.getOriginalFilename()), getFileName(file.getOriginalFilename())[1]));
        try(InputStream is = file.getInputStream()) {
            Files.copy(is, convFile.toPath());
        }
        return convFile;
    }

    private void cleanDisk(File file) {
        try {
            Path filePath = file.toPath();
            Files.delete(filePath);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public String generatePublicValue(String originalName){
        String fileName = getFileName(originalName)[0];
        return StringUtils.join(UUID.randomUUID().toString(), "_", fileName);
    }

    public String[] getFileName(String originalName) {
        return originalName.split("\\.");
    }
}
