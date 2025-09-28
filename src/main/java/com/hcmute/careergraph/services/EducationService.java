package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.EducationDto;
import com.hcmute.careergraph.persistence.dtos.request.EducationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EducationService {

    EducationDto createEducation(EducationRequest request);

    EducationDto getEducationById(String id);

    Page<EducationDto> getAllEducations(Pageable pageable);

    EducationDto updateEducation(String id, EducationRequest request);

    void deleteEducation(String id);

    void activateEducation(String id);

    void deactivateEducation(String id);
}
