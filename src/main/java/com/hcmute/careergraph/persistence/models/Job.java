package com.hcmute.careergraph.persistence.models;

import com.hcmute.careergraph.enums.EmploymentType;
import com.hcmute.careergraph.enums.JobCategory;
import com.hcmute.careergraph.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Job extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "salary_range")
    private String salaryRange;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "work_arrangement")
    private String workArrangement;

    @Column(name = "posted_date")
    private String postedDate;

    @Column(name = "expiry_date")
    private String expiryDate;

    @Column(name = "number_of_positions")
    private Integer numberOfPositions;

    @Column(name = "work_location")
    private String workLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "is_urgent")
    private Boolean isUrgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_category")
    private JobCategory jobCategory;

    // Many-to-One relationship with Company
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // One-to-Many relationship with JobSkill
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JobSkill> requiredSkills;

    // One-to-Many relationship with Application
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Application> applications;
}

