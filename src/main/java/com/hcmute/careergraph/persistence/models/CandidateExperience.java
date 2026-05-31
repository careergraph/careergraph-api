package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import com.hcmute.careergraph.enums.common.ConstDefault;

@Entity
@Table(name = "candidate_experience")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"candidate", "company"})
@EqualsAndHashCode(callSuper = true, exclude = {"candidate", "company"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateExperience extends BaseEntity {

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "salary")
    private Integer salary;

    @Column(name = "job_title")
    @Builder.Default
    private String jobTitle = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "description", columnDefinition = "TEXT")
    @Builder.Default
    private String description = ConstDefault.EMPTY_STRING.getValue();

    // Many-to-One relationship with Candidate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    // Many-to-One relationship with Company
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;
}