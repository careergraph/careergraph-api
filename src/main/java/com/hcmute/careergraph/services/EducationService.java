package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.EducationRequest;
import com.hcmute.careergraph.persistence.models.Education;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EducationService {

    Education createEducation(EducationRequest request);

    Education getEducationById(String id);

    Page<Education> getAllEducations(Pageable pageable);

    Education updateEducation(String id, EducationRequest request);

    void deleteEducation(String id);

    void activateEducation(String id);

    void deactivateEducation(String id);
}
