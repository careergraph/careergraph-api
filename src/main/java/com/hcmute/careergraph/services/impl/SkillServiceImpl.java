package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.mapper.SkillMapper;
import com.hcmute.careergraph.persistence.dtos.response.SkillDto;
import com.hcmute.careergraph.persistence.dtos.request.SkillRequest;
import com.hcmute.careergraph.persistence.models.Skill;
import com.hcmute.careergraph.repositories.SkillRepository;
import com.hcmute.careergraph.services.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Override
    public SkillDto createSkill(SkillRequest request) {
        log.info("Creating new skill with name: {}", request.getName());
        
        Skill skill = Skill.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();

        Skill savedSkill = skillRepository.save(skill);
        log.info("Skill created successfully with id: {}", savedSkill.getId());
        
        return skillMapper.toDto(savedSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillDto getSkillById(String id) {
        log.info("Getting skill by id: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
        return skillMapper.toDto(skill);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SkillDto> getAllSkills(Pageable pageable) {
        log.info("Getting all skills with pagination");
        Page<Skill> skills = skillRepository.findAll(pageable);
        return skills.map(skillMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SkillDto> getSkillsByCategory(String category, Pageable pageable) {
        log.info("Getting skills by category: {}", category);
        Page<Skill> skills = skillRepository.findByCategory(category, pageable);
        return skills.map(skillMapper::toDto);
    }

    @Override
    public SkillDto updateSkill(String id, SkillRequest request) {
        log.info("Updating skill with id: {}", id);
        
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));

        // Update fields
        skill.setName(request.getName());
        skill.setCategory(request.getCategory());
        skill.setDescription(request.getDescription());

        Skill updatedSkill = skillRepository.save(skill);
        log.info("Skill updated successfully with id: {}", updatedSkill.getId());
        
        return skillMapper.toDto(updatedSkill);
    }

    @Override
    public void deleteSkill(String id) {
        log.info("Deleting skill with id: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
        skill.softDelete();
        skillRepository.save(skill);
        log.info("Skill soft deleted successfully with id: {}", id);
    }

    @Override
    public void activateSkill(String id) {
        log.info("Activating skill with id: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
        skill.activate();
        skillRepository.save(skill);
        log.info("Skill activated successfully with id: {}", id);
    }

    @Override
    public void deactivateSkill(String id) {
        log.info("Deactivating skill with id: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
        skill.deactivate();
        skillRepository.save(skill);
        log.info("Skill deactivated successfully with id: {}", id);
    }
}
