package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.AddressDto;
import com.hcmute.careergraph.persistence.models.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "addressId", source = "id")
    @Mapping(target = "partyId", source = "party.id")
    AddressDto toDto(Address address);
}
