package com.hcmute.careergraph.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {

    String uploadImage(MultipartFile file) throws IOException;

    String uploadVideo(MultipartFile file) throws IOException;
}
