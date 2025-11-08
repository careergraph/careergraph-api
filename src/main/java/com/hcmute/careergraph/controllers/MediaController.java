package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.persistence.dtos.response.CloudFileResponse;
import com.hcmute.careergraph.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload an image (avatar/cover...). Required request params:
     * - ownerType (e.g. "candidates" or "companies")
     * - idd       (owner id; note: name "idd" used per your request)
     * - fileType  (e.g. "avatar", "cover")
     * <p>
     * Example form-data keys: file, ownerType=candidates, idd=123, fileType=avatar
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") String ownerType,
            @RequestParam("idd") String idd,
            @RequestParam("fileType") FileType fileType
    ) throws IOException {
        String url = cloudinaryService.uploadImage(file, ownerType, idd, fileType);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("ownerType", ownerType);
        response.put("idd", idd);
        response.put("fileType", fileType.name());
        return ResponseEntity.ok(response);
    }

    /**
     * Upload a video (resource_type = "video").
     */
    @PostMapping("/video")
    public ResponseEntity<Map<String, String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") String ownerType,
            @RequestParam("idd") String idd,
            @RequestParam("fileType") FileType fileType
    ) throws IOException {
        String url = cloudinaryService.uploadVideo(file, ownerType, idd, fileType);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("ownerType", ownerType);
        response.put("idd", idd);
        response.put("fileType", fileType.name());
        return ResponseEntity.ok(response);
    }

    /**
     * Upload a generic file (cv, document...). Use fileType e.g. "cv".
     */
    @PostMapping("/file")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") String ownerType,
            @RequestParam("idd") String idd,
            @RequestParam("fileType") FileType fileType
    ) throws IOException {
        String url = cloudinaryService.uploadFile(file, ownerType, idd, fileType);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("ownerType", ownerType);
        response.put("idd", idd);
        response.put("fileType", fileType.name());
        return ResponseEntity.ok(response);
    }

    /**
     * List files for an owner. If fileType is omitted, returns all files for the owner.
     * Example: GET /media?ownerType=candidates&idd=123
     * GET /media?ownerType=candidates&idd=123&fileType=cv
     */
    @GetMapping
    public ResponseEntity<List<CloudFileResponse>> listFiles(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("idd") String idd,
            @RequestParam(value = "fileType", required = false) FileType fileType
    ) throws IOException {
        List<CloudFileResponse> files = cloudinaryService.listFiles(ownerType, idd, fileType);
        return ResponseEntity.ok(files);
    }

    /**
     * Delete a resource by its Cloudinary public_id.
     * Example: DELETE /media?publicId=candidates/123/avatar/uuid_name
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteByPublicId(@RequestParam("publicId") String publicId) throws IOException {
        boolean deleted = cloudinaryService.deleteByPublicId(publicId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("publicId", publicId);
        resp.put("deleted", deleted);
        return ResponseEntity.ok(resp);
    }
}