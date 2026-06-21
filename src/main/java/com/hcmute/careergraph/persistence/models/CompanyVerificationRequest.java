package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "company_verification_requests")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true, exclude = {"company", "submittedByAccount", "reviewedByAccount", "documents"})
@EqualsAndHashCode(callSuper = true, exclude = {"company", "submittedByAccount", "reviewedByAccount", "documents"})
public class CompanyVerificationRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private CompanyVerificationStatus verificationStatus;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "legal_representative_name", length = 255)
    private String legalRepresentativeName;

    @Column(name = "business_email", length = 255)
    private String businessEmail;

    @Column(name = "website", length = 500)
    private String website;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_account_id")
    private Account submittedByAccount;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_account_id")
    private Account reviewedByAccount;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @OneToMany(mappedBy = "verificationRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CompanyVerificationDocument> documents = new LinkedHashSet<>();

    public void replaceDocuments(Set<CompanyVerificationDocument> nextDocuments) {
        documents.clear();
        if (nextDocuments == null) {
            return;
        }
        nextDocuments.forEach(this::addDocument);
    }

    public void addDocument(CompanyVerificationDocument document) {
        if (document == null) {
            return;
        }
        documents.add(document);
        document.setVerificationRequest(this);
    }
}
