package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import com.hcmute.careergraph.enums.common.ConstDefault;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "companies")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true, exclude = {"account", "jobs", "companyConnections"})
@EqualsAndHashCode(callSuper = true, exclude = {"account", "jobs", "companyConnections"})
public class Company extends Party {

    @Column(name = "size")
    @Builder.Default
    private String size = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "name")
    @Builder.Default
    private String name = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "website")
    @Builder.Default
    private String website = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "ceo_name")
    @Builder.Default
    private String ceoName = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "description", columnDefinition = "TEXT")
    @Builder.Default
    private String description = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "no_of_members", columnDefinition = "int default 0")
    private Integer noOfMembers;

    @Column(name = "year_founded")
    private Integer yearFounded;

    @Column(name = "offer_before_trial", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean offerBeforeTrial = true;

    @Column(name = "enable_offboarded_stage", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean enableOffboardedStage = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private CompanyVerificationStatus verificationStatus = CompanyVerificationStatus.NOT_SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 50)
    @Builder.Default
    private CompanyOperationalStatus operationalStatus = CompanyOperationalStatus.ACTIVE;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "legal_representative_name", length = 255)
    private String legalRepresentativeName;

    @Column(name = "verification_business_email", length = 255)
    private String verificationBusinessEmail;

    @Column(name = "verification_website", length = 500)
    private String verificationWebsite;

    @Column(name = "verification_submitted_at")
    private LocalDateTime verificationSubmittedAt;

    @Column(name = "verification_reviewed_at")
    private LocalDateTime verificationReviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_reviewed_by_account_id")
    private Account verificationReviewedByAccount;

    @Column(name = "verification_admin_note", columnDefinition = "TEXT")
    private String verificationAdminNote;

    @Column(name = "block_reason", columnDefinition = "TEXT")
    private String blockReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_account_id")
    private Account blockedByAccount;

    @Column(name = "unblocked_at")
    private LocalDateTime unblockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unblocked_by_account_id")
    private Account unblockedByAccount;

    // Account
    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Account account;

    // One-to-Many relationship with Job
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Job> jobs;

    // Connections with companies
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "connected_company_id")
    private Set<Connection> companyConnections;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CompanyRecruitmentStage> recruitmentStages;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CompanyVerificationRequest> verificationRequests = new LinkedHashSet<>();
}
