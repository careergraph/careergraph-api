package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ApplicationDto;
import com.hcmute.careergraph.persistence.models.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "applicationId", source = "id")
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "job", ignore = true)
    ApplicationDto toDto(Application application);
}
