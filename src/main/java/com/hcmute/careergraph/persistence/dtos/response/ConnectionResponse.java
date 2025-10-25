package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.candidate.ConnectionType;
import lombok.Builder;

@Builder
public record ConnectionResponse(
        String connectionId,
        String note,
        ConnectionType connectionType,
        Boolean hasSeen,
        Boolean disableNotification,
        CandidateResponse candidate,
        String connectedCandidateId
) {
}
