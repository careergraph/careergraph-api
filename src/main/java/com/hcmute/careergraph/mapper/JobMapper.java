package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.enums.JobCategory;
import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.models.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "jobId", source = "id")
    @Mapping(target = "requiredSkills", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "jobCategory", ignore = true)
    JobDto toDto(Job job);

//    default Map<String, Object> map(JobCategory value) {
//        if (value == null) return null;
//        Map<String, Object> map = new HashMap<>();
//        map.put("displayName", value.name());
//        map.put("description", value.ordinal());
//        return map;
//    }
}
