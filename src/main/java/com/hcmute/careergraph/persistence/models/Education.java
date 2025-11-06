package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.education.EducationLevel;
import com.hcmute.careergraph.enums.education.UniversityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "educations",
        indexes = {
                @Index(name = "idx_educations_official_name", columnList = "official_name"),
                @Index(name = "idx_educations_short_name", columnList = "short_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_educations_official_name", columnNames = {"official_name"})
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EntityListeners(AuditingEntityListener.class)
public class Education extends Party {

    @Column(name = "official_name", nullable = false, length = 255)
    private String officialName;

    @Column(name = "short_name", length = 64)
    private String shortName;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "university_type", length = 32)
    private UniversityType universityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 32)
    private EducationLevel level;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "accreditation", length = 128)
    private String accreditation;

    @OneToMany(mappedBy = "education", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CandidateEducation> candidateEducations = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "connected_education_id")
    private Set<Connection> educationConnections = new HashSet<>();

    /* ---------------- Helper ---------------- */
    public void addCandidateEducation(CandidateEducation ce) {
        if (candidateEducations == null) candidateEducations = new HashSet<>();
        candidateEducations.add(ce);
        ce.setEducation(this);
    }

    public void removeCandidateEducation(CandidateEducation ce) {
        if (candidateEducations != null) {
            candidateEducations.remove(ce);
        }
        ce.setEducation(null);
    }
}
