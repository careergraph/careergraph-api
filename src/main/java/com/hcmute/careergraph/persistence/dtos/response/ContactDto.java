package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.candidate.ContactType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactDto {

    private String contactId;

    private String value;

    private Boolean verified;

    private Boolean isPrimary;

    private ContactType type;

    private String partyId;
}
