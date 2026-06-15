package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.common.Status;
import lombok.Builder;

@Builder
public record JobSettingsUpdateRequest(
        Boolean aiScreeningEnabled,
        Status status,
        String expiryDate) {

    public boolean hasUpdates() {
        return aiScreeningEnabled != null
                || status != null
                || (expiryDate != null && !expiryDate.isBlank());
    }
}
