package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.candidate.ContactType;
import lombok.Builder;

@Builder
public record ContactResponse(
        String contactId,
        String value,
        Boolean verified,
        Boolean isPrimary,
        ContactType contactType,
        String partyId
) {
}
