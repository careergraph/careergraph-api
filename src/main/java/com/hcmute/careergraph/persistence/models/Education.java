package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "educations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Education extends BaseEntity {

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_currently_studying")
    private Boolean isCurrentlyStudying;

    // One-to-Many relationship with CandidateEducation
    @OneToMany(mappedBy = "education", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CandidateEducation> candidateEducations;

    // Connections with educations
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "connected_education_id")
    private Set<Connection> educationConnections;
}
