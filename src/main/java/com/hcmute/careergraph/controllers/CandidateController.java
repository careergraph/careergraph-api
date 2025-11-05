package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CandidateMapper;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateClientResponse;
import com.hcmute.careergraph.persistence.dtos.response.CandidateResponse;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.services.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final CandidateMapper candidateMapper;
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
    public RestResponse<CandidateResponse> getMyProfile() throws ChangeSetPersister.NotFoundException {
        Candidate candidate = candidateService.getMyProfile(securityUtils.getCandidateId().get());

        return RestResponse.<CandidateResponse>builder()
                .status(HttpStatus.OK)
                .data(candidateMapper.toResponse(candidate))
                .build();
    }
    @PostMapping("/update-information")
    public RestResponse<CandidateClientResponse.CandidateProfileResponse> updateInformation(@Valid @RequestBody CandidateRequest.UpdateInformationRequest request) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.updateInformation(securityUtils.getCandidateId().get(), request);

        return RestResponse.<CandidateClientResponse.CandidateProfileResponse>builder()
                .status(HttpStatus.OK)
                .data(candidateMapper.toProfileResponse(candidate))
                .build();
    }


    @PostMapping("/update-job-find-criteria")
    public RestResponse<CandidateClientResponse.CandidateJobCriteriaResponse> updateJobFindCriteriaInfo(@Valid @RequestBody CandidateRequest.UpdateJobCriteriaRequest request) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.updateJobFindCriteriaInfo(securityUtils.getCandidateId().get(), request);

        return RestResponse.<CandidateClientResponse.CandidateJobCriteriaResponse>builder()
                .status(HttpStatus.OK)
                .data(candidateMapper.toJobCriteriaResponse(candidate))
                .build();
    }


    @GetMapping("/general-info")
    public RestResponse<CandidateClientResponse.GeneralInfoResponse> getGeneralInfo() throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.getMyProfile(securityUtils.getCandidateId().get());

        return RestResponse.<CandidateClientResponse.GeneralInfoResponse>builder()
                .status(HttpStatus.OK)
                .data(candidateMapper.toGeneralInfoResponse(candidate))
                .build();
    }

    @PostMapping("/update-general-info")
    public RestResponse<CandidateResponse> getGeneralInfo(@Valid @RequestBody CandidateRequest.UpdateGeneralInfo request) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.updateGeneralInfo(securityUtils.getCandidateId().get(), request);

        return RestResponse.<CandidateResponse>builder()
                .status(HttpStatus.OK)
                .data(candidateMapper.toResponse(candidate))
                .build();
    }
}
