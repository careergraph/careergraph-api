package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.SkillDto;
import com.hcmute.careergraph.persistence.dtos.request.SkillRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SkillService {

    SkillDto createSkill(SkillRequest request);

    SkillDto getSkillById(String id);

    Page<SkillDto> getAllSkills(Pageable pageable);

    Page<SkillDto> getSkillsByCategory(String category, Pageable pageable);

    SkillDto updateSkill(String id, SkillRequest request);

    void deleteSkill(String id);

    void activateSkill(String id);

    void deactivateSkill(String id);
}
