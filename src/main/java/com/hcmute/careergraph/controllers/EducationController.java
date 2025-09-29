package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.response.EducationDto;
import com.hcmute.careergraph.persistence.dtos.request.EducationRequest;
import com.hcmute.careergraph.services.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("educations")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @PostMapping
    public RestResponse<EducationDto> createEducation(@Valid @RequestBody EducationRequest request) {
        EducationDto education = educationService.createEducation(request);
        return RestResponse.<EducationDto>builder()
                .status(HttpStatus.CREATED)
                .message("Education created successfully")
                .data(education)
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<EducationDto> getEducationById(@PathVariable String id) {
        EducationDto education = educationService.getEducationById(id);
        return RestResponse.<EducationDto>builder()
                .status(HttpStatus.OK)
                .message("Education retrieved successfully")
                .data(education)
                .build();
    }

    @GetMapping
    public RestResponse<Page<EducationDto>> getAllEducations(Pageable pageable) {
        Page<EducationDto> educations = educationService.getAllEducations(pageable);
        return RestResponse.<Page<EducationDto>>builder()
                .status(HttpStatus.OK)
                .message("Educations retrieved successfully")
                .data(educations)
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<EducationDto> updateEducation(@PathVariable String id, @Valid @RequestBody EducationRequest request) {
        EducationDto education = educationService.updateEducation(id, request);
        return RestResponse.<EducationDto>builder()
                .status(HttpStatus.OK)
                .message("Education updated successfully")
                .data(education)
                .build();
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteEducation(@PathVariable String id) {
        educationService.deleteEducation(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Education deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/activate")
    public RestResponse<Void> activateEducation(@PathVariable String id) {
        educationService.activateEducation(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Education activated successfully")
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    public RestResponse<Void> deactivateEducation(@PathVariable String id) {
        educationService.deactivateEducation(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Education deactivated successfully")
                .build();
    }
}
