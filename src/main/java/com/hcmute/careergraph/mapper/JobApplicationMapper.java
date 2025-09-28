package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.JobApplicationDto;
import com.hcmute.careergraph.persistence.models.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {JobMapper.class, CandidateMapper.class})
public interface JobApplicationMapper {

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "candidateId", source = "candidate.id")
    JobApplicationDto toDto(JobApplication entity);

    @Mapping(target = "job", ignore = true)
    @Mapping(target = "candidate", ignore = true)
    JobApplication toEntity(JobApplicationDto dto);
}
