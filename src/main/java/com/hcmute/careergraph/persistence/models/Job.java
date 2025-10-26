package com.hcmute.careergraph.persistence.models;

import com.hcmute.careergraph.enums.work.EducationType;
import com.hcmute.careergraph.enums.work.EmploymentType;
import com.hcmute.careergraph.enums.work.ExperienceLevel;
import com.hcmute.careergraph.enums.work.JobCategory;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.JsonUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Job extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "department")
    private String department;

    /**
     * Field JSON for converter (UI Job): responsibilities
     */
    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private List<String> responsibilities;

    /**
     * Field JSON for converter (UI Job): qualifications
     */
    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qualifications", columnDefinition = "TEXT")
    private List<String> qualifications;

    // ===== Thêm field minimumQualifications =====
    /**
     * Field JSON for converter (UI Job): minimumQualifications
     */
    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "minimum_qualifications", columnDefinition = "TEXT")
    private List<String> minimumQualifications;

    /**
     * Field JSON for converter (UI Job): benefits
     */
    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benefits", columnDefinition = "TEXT")
    private List<String> benefits;

    /**
     * Fields JOB detail
     */
    @Column(name = "salary_range")
    private String salaryRange;

    @Column(name = "min_experience")
    private Integer minExperience;

    @Column(name = "max_experience")
    private Integer maxExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_category")
    private JobCategory jobCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "education")
    private EducationType education;

    /**
     * Post information for JOB
     */
    @Column(name = "posted_date")
    private String postedDate;

    @Column(name = "expiry_date")
    private String expiryDate;

    @Column(name = "number_of_positions")
    private Integer numberOfPositions;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "promotion_type")
    private String promotionType; // "free" or "paid"

    /**
     * Address for JOB - CẬP NHẬT: Thêm state (tỉnh/thành phố)
     * Cấu trúc: state (tỉnh) -> city (quận/huyện) -> district (phường/xã) -> address (địa chỉ cụ thể)
     */
    @Column(name = "state")
    private String state; // Tỉnh/Thành phố (code từ API location)

    @Column(name = "city")
    private String city; // Quận/Huyện (code từ API location)

    @Column(name = "district")
    private String district; // Phường/Xã (code từ API location)

    @Column(name = "address")
    private String address; // Địa chỉ cụ thể

    @Column(name = "remote_job")
    private boolean remoteJob;

    /**
     * Stats fields for JOB
     */
    @Column(name = "views")
    private Integer views = 0;

    @Column(name = "applicants")
    private Integer applicants = 0;

    @Column(name = "saved")
    private Integer saved = 0;

    @Column(name = "liked")
    private Integer liked = 0;

    @Column(name = "shared")
    private Integer shared = 0;

    // ===== Thêm status field =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE; // Default là ACTIVE khi tạo mới

    // Many-to-One relationship with Company
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // One-to-Many relationship with Application
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Application> applications = new HashSet<>();

    @Override
    public void prePersist() {
        super.prePersist();

        if (postedDate == null) {
            postedDate = LocalDate.now().toString();
        }
    }
}

