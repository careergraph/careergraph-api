package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateDto;
import com.hcmute.careergraph.services.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final SecurityUtils securityUtils;

    @PostMapping("/{id}/files")
    public RestResponse<String> uploadFile(
            @PathVariable String id,
            @RequestParam("type") FileType type,
            @RequestParam("file") MultipartFile file) throws ChangeSetPersister.NotFoundException {

        String objectName = candidateService.updateResource(id, file, type);
        return RestResponse.<String>builder()
                .status(HttpStatus.CREATED)
                .message("Update resource successfully")
                .data(objectName)
                .build();
    }

    @GetMapping("/{id}/files")
    public RestResponse getFileUrl(
            @PathVariable String id,
            @RequestParam("type") FileType type) throws ChangeSetPersister.NotFoundException {

        String url = candidateService.getResource(id, type);
        return RestResponse.<String>builder()
                .status(HttpStatus.OK)
                .message("Get resource successfully")
                .data(url)
                .build();
    }

    @GetMapping("/me")
    public RestResponse<CandidateDto> getMyProfile() throws ChangeSetPersister.NotFoundException {
        return RestResponse.<CandidateDto>builder()
                .status(HttpStatus.OK)
                .data(candidateService.getMyProfile(securityUtils.getCandidateId().get()))
                .build();
    }
    @PostMapping("/update-information")
    public RestResponse<CandidateDto> updateInformation(@Valid @RequestBody CandidateRequest.UpdateInformation request) throws ChangeSetPersister.NotFoundException{
        return RestResponse.<CandidateDto>builder()
                .status(HttpStatus.OK)
                .data(candidateService.updateInformation(securityUtils.getCandidateId().get(), request))
                .build();
    }
}
