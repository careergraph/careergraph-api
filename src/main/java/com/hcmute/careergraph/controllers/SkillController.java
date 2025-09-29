package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.persistence.dtos.response.SkillDto;
import com.hcmute.careergraph.persistence.dtos.request.SkillRequest;
import com.hcmute.careergraph.services.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public RestResponse<SkillDto> createSkill(@Valid @RequestBody SkillRequest request) {
        SkillDto skill = skillService.createSkill(request);
        return RestResponse.<SkillDto>builder()
                .status(HttpStatus.CREATED)
                .message("Skill created successfully")
                .data(skill)
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<SkillDto> getSkillById(@PathVariable String id) {
        SkillDto skill = skillService.getSkillById(id);
        return RestResponse.<SkillDto>builder()
                .status(HttpStatus.OK)
                .message("Skill retrieved successfully")
                .data(skill)
                .build();
    }

    @GetMapping
    public RestResponse<Page<SkillDto>> getAllSkills(Pageable pageable) {
        Page<SkillDto> skills = skillService.getAllSkills(pageable);
        return RestResponse.<Page<SkillDto>>builder()
                .status(HttpStatus.OK)
                .message("Skills retrieved successfully")
                .data(skills)
                .build();
    }

    @GetMapping("/category/{category}")
    public RestResponse<Page<SkillDto>> getSkillsByCategory(@PathVariable String category, Pageable pageable) {
        Page<SkillDto> skills = skillService.getSkillsByCategory(category, pageable);
        return RestResponse.<Page<SkillDto>>builder()
                .status(HttpStatus.OK)
                .message("Skills retrieved successfully")
                .data(skills)
                .build();
    }

    @PutMapping("/{id}")
    public RestResponse<SkillDto> updateSkill(@PathVariable String id, @Valid @RequestBody SkillRequest request) {
        SkillDto skill = skillService.updateSkill(id, request);
        return RestResponse.<SkillDto>builder()
                .status(HttpStatus.OK)
                .message("Skill updated successfully")
                .data(skill)
                .build();
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteSkill(@PathVariable String id) {
        skillService.deleteSkill(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Skill deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/activate")
    public RestResponse<Void> activateSkill(@PathVariable String id) {
        skillService.activateSkill(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Skill activated successfully")
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    public RestResponse<Void> deactivateSkill(@PathVariable String id) {
        skillService.deactivateSkill(id);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Skill deactivated successfully")
                .build();
    }
}
