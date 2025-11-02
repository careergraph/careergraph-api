package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CandidateExperienceMapper;
import com.hcmute.careergraph.mapper.CandidateMapper;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CandidateClientResponse;
import com.hcmute.careergraph.persistence.dtos.response.CandidateExperienceResponse;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final CandidateMapper candidateMapper;
    private final SecurityUtils securityUtils;
    private final CandidateExperienceMapper candidateExperienceMapper;

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

    @PostMapping("/update-general-info")
    public RestResponse<CandidateResponse> updateGeneralInfo(@Valid @RequestBody CandidateRequest.UpdateGeneralInfo request) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.updateGeneralInfo(securityUtils.getCandidateId().get(), request);

        return RestResponse.<CandidateResponse>builder()
                .status(HttpStatus.OK)
                .data(candidateMapper.toResponse(candidate))
                .build();
    }

    @PostMapping("/experiences")
    public RestResponse<Set<CandidateExperienceResponse>> addExperience(@Valid @RequestBody CandidateRequest.CandidateExperienceRequest request) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.addExperience(securityUtils.getCandidateId().get(), request);
        return RestResponse.<Set<CandidateExperienceResponse>>builder()
                .status(HttpStatus.OK)
                .data(candidateExperienceMapper.toResponses(candidate.getExperiences()))
                .build();
    }
    @PutMapping("/experiences/{experienceId}")
    public RestResponse<Set<CandidateExperienceResponse>> updateExperience(@PathVariable String experienceId ,@Valid @RequestBody CandidateRequest.CandidateExperienceRequest request) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.updateExperience(securityUtils.getCandidateId().get(), experienceId, request);
        return RestResponse.<Set<CandidateExperienceResponse>>builder()
                .status(HttpStatus.OK)
                .data(candidateExperienceMapper.toResponses(candidate.getExperiences()))
                .build();
    }

    @DeleteMapping("/experiences/{experienceId}")
    public RestResponse<Set<CandidateExperienceResponse>> deleteExperience(@PathVariable String experienceId) throws ChangeSetPersister.NotFoundException{
        Candidate candidate = candidateService.deleteExperience(securityUtils.getCandidateId().get(), experienceId);
        return RestResponse.<Set<CandidateExperienceResponse>>builder()
                .status(HttpStatus.OK)
                .data(candidateExperienceMapper.toResponses(candidate.getExperiences()))
                .build();
    }
}
