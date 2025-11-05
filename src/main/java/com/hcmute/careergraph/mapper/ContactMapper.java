package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ContactResponse;
import com.hcmute.careergraph.persistence.models.BaseEntity;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Contact;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ContactMapper {
    public ContactResponse toResponse(Contact contact){
        if(contact == null) return ContactResponse.builder().build();

        return ContactResponse.builder()
                .contactId(contact.getId())
                .value(contact.getValue())
                .contactType(contact.getContactType())
                .isPrimary(contact.getIsPrimary())
                .verified(contact.getVerified())
                .build();
    }

    public Set<ContactResponse> toResponses(Set<Contact> contacts){
        if(contacts == null) return new HashSet<>();

        return contacts.stream()
                .filter(BaseEntity::isActive)
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }
}
