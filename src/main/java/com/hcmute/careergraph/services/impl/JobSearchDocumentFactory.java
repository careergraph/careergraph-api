package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.helper.VietnamProvinceUtils;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class JobSearchDocumentFactory {

    private final CompanyAccessPolicyService companyAccessPolicyService;

    public boolean shouldIndex(Job job) {
        return companyAccessPolicyService.isJobPubliclyAvailable(job);
    }

    public String buildEmbeddingText(Job job) {
        if (job == null) {
            return "";
        }

        return Stream.of(
                        safe(job.getTitle()),
                        safe(job.getDescription()),
                        safe(job.getDepartment()),
                        enumDisplay(job.getJobCategory()),
                        enumName(job.getEmploymentType()),
                        enumName(job.getExperienceLevel()),
                        enumName(job.getEducation()),
                        safe(job.getState()),
                        safe(job.getCity()),
                        safe(job.getDistrict()),
                        safe(job.getAddress()),
                        join(job.getQualifications()),
                        join(job.getMinimumQualifications()),
                        join(job.getResponsibilities()),
                        join(job.getBenefits()))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    public String buildContentHash(Job job) {
        if (job == null) {
            return hashText("");
        }

        Company company = job.getCompany();
        return hashText(Stream.of(
                        safe(job.getId()),
                        safe(job.getTitle()),
                        safe(job.getDescription()),
                        safe(job.getDepartment()),
                        safe(job.getStatus() != null ? job.getStatus().name() : null),
                        safe(job.getSalaryRange()),
                        safe(job.getPostedDate()),
                        safe(job.getExpiryDate()),
                        safe(job.getPromotionType()),
                        safe(job.getContactEmail()),
                        safe(job.getContactPhone()),
                        safe(job.getState()),
                        safe(job.getCity()),
                        safe(job.getDistrict()),
                        safe(job.getAddress()),
                        Boolean.toString(job.isRemoteJob()),
                        String.valueOf(job.getAiScreeningEnabled()),
                        String.valueOf(job.getResume()),
                        String.valueOf(job.getCoverLetter()),
                        String.valueOf(job.getMinExperience()),
                        String.valueOf(job.getMaxExperience()),
                        String.valueOf(job.getNumberOfPositions()),
                        enumName(job.getJobCategory()),
                        enumName(job.getEmploymentType()),
                        enumName(job.getExperienceLevel()),
                        enumName(job.getEducation()),
                        company == null ? "" : safe(company.getId()),
                        company == null ? "" : enumName(company.getVerificationStatus()),
                        company == null ? "" : enumName(company.getOperationalStatus()),
                        join(job.getQualifications()),
                        join(job.getMinimumQualifications()),
                        join(job.getResponsibilities()),
                        join(job.getBenefits()),
                        buildEmbeddingText(job))
                .collect(Collectors.joining("|")));
    }

    public JobES toDocument(Job job, float[] embedding) {
        Company company = job.getCompany();
        CompanyVerificationStatus verificationStatus = company != null && company.getVerificationStatus() != null
                ? company.getVerificationStatus()
                : CompanyVerificationStatus.NOT_SUBMITTED;
        CompanyOperationalStatus operationalStatus = company != null && company.getOperationalStatus() != null
                ? company.getOperationalStatus()
                : CompanyOperationalStatus.ACTIVE;

        return JobES.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .status(enumName(job.getStatus()))
                .jobCategory(enumName(job.getJobCategory()))
                .employmentType(enumName(job.getEmploymentType()))
                .experienceLevel(enumName(job.getExperienceLevel()))
                .education(enumName(job.getEducation()))
                .state(job.getState())
                .provinceSlug(VietnamProvinceUtils.slugFromStateName(job.getState()))
                .provinceCode(VietnamProvinceUtils.codeFromStateName(job.getState()))
                .city(job.getCity())
                .companyId(company != null ? company.getId() : null)
                .companyVerificationStatus(verificationStatus.name())
                .companyOperationalStatus(operationalStatus.name())
                .companyBlocked(operationalStatus == CompanyOperationalStatus.BLOCKED)
                .jobSearchable(shouldIndex(job))
                .qualifications(job.getQualifications())
                .minimumQualifications(job.getMinimumQualifications())
                .responsibilities(job.getResponsibilities())
                .createdAt(job.getCreatedDate() != null ? job.getCreatedDate().toLocalDate() : LocalDate.now())
                .contentHash(buildContentHash(job))
                .embedding(embedding)
                .build();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String enumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private String enumDisplay(com.hcmute.careergraph.enums.job.JobCategory jobCategory) {
        return jobCategory != null ? jobCategory.getDisplayName() : null;
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
