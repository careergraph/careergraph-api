package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ConnectionDto;
import com.hcmute.careergraph.persistence.models.Connection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConnectionMapper {

    @Mapping(target = "connectionId", source = "connectedCandidateId")
    ConnectionDto toDto(Connection connection);
}
