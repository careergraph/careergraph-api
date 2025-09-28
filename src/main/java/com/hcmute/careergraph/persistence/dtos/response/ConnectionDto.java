package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.ConnectionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDto {

    private String connectionId;

    private String note;

    private ConnectionType connectionType;

    private Boolean hasSeen;

    private Boolean disableNotification;

    private CandidateDto candidate;

    private String connectedCandidateId;

    // private String connectedCompanyId;

    // private String connectedEducationId;
}
