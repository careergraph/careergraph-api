package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.helper.JsonUtils;
import jakarta.persistence.*;
import com.hcmute.careergraph.enums.common.ConstDefault;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
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
@ToString(callSuper = true, exclude = {"account", "connections", "educations", "experiences", "skills", "applications", "contacts", "addresses", "saved_jobs"})
@EqualsAndHashCode(callSuper = true, exclude = {"account", "connections", "educations", "experiences", "skills", "applications"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate extends Party {

    @Column(name = "first_name")
    @Builder.Default
    private String firstName = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "last_name")
    @Builder.Default
    private String lastName = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "date_of_birth")
    @Builder.Default
    private String dateOfBirth = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "gender")
    @Builder.Default
    private String gender = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "current_job_title")
    @Builder.Default
    private String currentJobTitle = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "desired_position")
    @Builder.Default
    private String desiredPosition = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "current_company")
    @Builder.Default
    private String currentCompany = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "industry")
    @Builder.Default
    private String industry = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "work_location")
    @Builder.Default
    private String workLocation = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "is_open_to_work")
    @Builder.Default
    private Boolean isOpenToWork = true;

    @Column(name = "is_open_to_notify_new_job",
            columnDefinition = "BOOLEAN DEFAULT FALSE"
    )
    @Builder.Default
    private Boolean isOpenToNotifyNewJob = false;

    @Column(name = "summary", columnDefinition = "TEXT")
    @Builder.Default
    private String summary = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "is_married")
    @Builder.Default
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
    @Builder.Default
    private String educationLevel = ConstDefault.EMPTY_STRING.getValue();

    @Column(name="current_position")
    @Builder.Default
    private String currentPosition = ConstDefault.EMPTY_STRING.getValue();

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
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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


    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(name = "saved_jobs")
    private List<SavedJob> savedJobs = new ArrayList<>();

}
