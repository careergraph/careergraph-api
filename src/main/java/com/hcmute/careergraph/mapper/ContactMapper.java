package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ContactDto;
import com.hcmute.careergraph.persistence.models.Contact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    @Mapping(target = "contactId", source = "id")
    @Mapping(target = "partyId", source = "party.id")
    ContactDto toDto(Contact contact);
}
