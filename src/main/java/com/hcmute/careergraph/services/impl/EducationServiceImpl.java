package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.mapper.EducationMapper;
import com.hcmute.careergraph.persistence.dtos.response.EducationDto;
import com.hcmute.careergraph.persistence.dtos.request.EducationRequest;
import com.hcmute.careergraph.persistence.models.Education;
import com.hcmute.careergraph.repositories.EducationRepository;
import com.hcmute.careergraph.services.EducationService;
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
public class EducationServiceImpl implements EducationService {

    private final EducationRepository educationRepository;
    private final EducationMapper educationMapper;

    @Override
    public EducationDto createEducation(EducationRequest request) {
        log.info("Creating new education with tagname: {}", request.getTagname());
        
        Education education = Education.builder()
                .tagname(request.getTagname())
                .avatar(request.getAvatar())
                .cover(request.getCover())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .isCurrentlyStudying(request.getIsCurrentlyStudying() != null ? request.getIsCurrentlyStudying() : false)
                .build();

        Education savedEducation = educationRepository.save(education);
        log.info("Education created successfully with id: {}", savedEducation.getId());
        
        return educationMapper.toDto(savedEducation);
    }

    @Override
    @Transactional(readOnly = true)
    public EducationDto getEducationById(String id) {
        log.info("Getting education by id: {}", id);
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Education not found with id: " + id));
        return educationMapper.toDto(education);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EducationDto> getAllEducations(Pageable pageable) {
        log.info("Getting all educations with pagination");
        Page<Education> educations = educationRepository.findAll(pageable);
        return educations.map(educationMapper::toDto);
    }

    @Override
    public EducationDto updateEducation(String id, EducationRequest request) {
        log.info("Updating education with id: {}", id);
        
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Education not found with id: " + id));

        // Update fields
        education.setTagname(request.getTagname());
        education.setAvatar(request.getAvatar());
        education.setCover(request.getCover());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setDescription(request.getDescription());
        education.setIsCurrentlyStudying(request.getIsCurrentlyStudying() != null ? 
            request.getIsCurrentlyStudying() : education.getIsCurrentlyStudying());

        Education updatedEducation = educationRepository.save(education);
        log.info("Education updated successfully with id: {}", updatedEducation.getId());
        
        return educationMapper.toDto(updatedEducation);
    }

    @Override
    public void deleteEducation(String id) {
        log.info("Deleting education with id: {}", id);
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Education not found with id: " + id));
        education.softDelete();
        educationRepository.save(education);
        log.info("Education soft deleted successfully with id: {}", id);
    }

    @Override
    public void activateEducation(String id) {
        log.info("Activating education with id: {}", id);
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Education not found with id: " + id));
        education.activate();
        educationRepository.save(education);
        log.info("Education activated successfully with id: {}", id);
    }

    @Override
    public void deactivateEducation(String id) {
        log.info("Deactivating education with id: {}", id);
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Education not found with id: " + id));
        education.deactivate();
        educationRepository.save(education);
        log.info("Education deactivated successfully with id: {}", id);
    }
}
