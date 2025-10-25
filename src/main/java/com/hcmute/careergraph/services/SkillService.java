package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.record.SkillLookupResponse;
import com.hcmute.careergraph.persistence.dtos.request.SkillRequest;
import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SkillService {

    Skill createSkill(SkillRequest request);

    Skill getSkillById(String id);

    Page<Skill> getAllSkills(Pageable pageable);

    Page<Skill> getSkillsByCategory(String category, Pageable pageable);

    Skill updateSkill(String id, SkillRequest request);

    void deleteSkill(String id);

    void activateSkill(String id);

    void deactivateSkill(String id);

    List<SkillLookupResponse> lookupSkill(String query);
}
