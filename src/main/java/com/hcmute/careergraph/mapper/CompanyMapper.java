package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.CompanyDto;
import com.hcmute.careergraph.persistence.models.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "companyId", source = "id")
    @Mapping(target = "jobs", ignore = true)
    CompanyDto toDto(Company company);
}
