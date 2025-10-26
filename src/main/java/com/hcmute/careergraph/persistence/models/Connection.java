package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.candidate.ConnectionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "connections")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection extends BaseEntity {

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type")
    private ConnectionType connectionType;

    @Column(name = "has_seen")
    private Boolean hasSeen;

    @Column(name = "disable_notification")
    private Boolean disableNotification;

    // Many-to-One relationship with Candidate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    // Specific connections to different party types
    @Column(name = "connected_candidate_id")
    private String connectedCandidateId;

    @Column(name = "connected_company_id")
    private String connectedCompanyId;

    @Column(name = "connected_education_id")
    private String connectedEducationId;
}

