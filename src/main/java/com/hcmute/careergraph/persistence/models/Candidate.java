package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.helper.JsonUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.procedure.internal.Util;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate extends Party {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "current_job_title")
    private String currentJobTitle;

    @Column(name = "desired_position")
    private String desiredPosition;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(name = "industry")
    private String industry;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "work_location")
    private String workLocation;

    @Column(name = "is_open_to_work")
    private Boolean isOpenToWork;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "is_married")
    private Boolean isMarried = false;

    @Column(name = "salary_expectation_min")
    private Integer salaryExpectationMin;

    @Column(name = "salary_expectation_max")
    private Integer salaryExpectationMax;

    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resumes", columnDefinition = "TEXT")
    private List<String> resumes;

    @Column(name="education_level")
    private String educationLevel;

    @Column(name="current_position")
    private String currentPosition;

    // Account
    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Account account;

    // Connection relationships (bidirectional)
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Connection> connections;

    // Education relationships
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CandidateEducation> educations;

    // Work experience relationships
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CandidateExperience> experiences;

    // Skill relationships
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CandidateSkill> skills;

    // Application relationships
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Application> applications;

    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "industries")
    private List<String> industries = new ArrayList<>();

    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "locations")
    private List<String> locations = new ArrayList<>();

    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "work_types")
    private List<String> workTypes = new ArrayList<>();



}
