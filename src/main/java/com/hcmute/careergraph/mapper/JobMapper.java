package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.dtos.record.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.record.JobResponse;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.JobSkill;
import com.hcmute.careergraph.persistence.models.Skill;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobMapper {

    /**
     * Convert từ JobCreationRequest sang Job Entity
     * Sử dụng khi tạo job mới
     *
     * @param request DTO từ client
     * @param skills List Skill entities để tạo JobSkill relationships
     * @param company Company entity (lấy từ authenticated user hoặc context)
     * @return Job entity chưa lưu vào DB
     */
    public Job toEntity(JobCreationRequest request, List<Skill> skills, Company company) {
        Job job = new Job();

        // Basic Information
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setDepartment(request.department());

        // Arrays
        job.setResponsibilities(request.responsibilities() != null ? request.responsibilities() : Collections.emptyList());
        job.setQualifications(request.qualifications() != null ? request.qualifications() : Collections.emptyList());
        job.setMinimumQualifications(request.minimumQualifications() != null ? request.minimumQualifications() : Collections.emptyList());
        job.setBenefits(request.benefits() != null ? request.benefits() : Collections.emptyList());

        // Experience
        job.setMinExperience(request.minExperience());
        job.setMaxExperience(request.maxExperience());
        job.setExperienceLevel(request.experienceLevel());

        // Job Type & Category
        job.setEmploymentType(request.employmentType());
        job.setJobCategory(request.jobCategory());
        job.setEducation(request.education());

        // Location
        job.setRemoteJob(request.remoteJob() != null ? request.remoteJob() : false);
        job.setState(request.state());
        job.setCity(request.city());
        job.setDistrict(request.district());
        job.setAddress(request.address());

        // Compensation & Contact
        job.setSalaryRange(request.salaryRange());
        job.setContactEmail(request.contactEmail());
        job.setContactPhone(request.contactPhone());

        // Posting Information
        job.setPromotionType(request.promotionType() != null ? request.promotionType() : "free");
        job.setNumberOfPositions(request.numberOfPositions() != null ? request.numberOfPositions() : 1);
        job.setExpiryDate(request.expiryDate());
        job.setPostedDate(LocalDate.now().toString());

        // Stats - Initialize to 0
        job.setViews(0);
        job.setApplicants(0);
        job.setSaved(0);
        job.setLiked(0);
        job.setShared(0);

        // Status - Default ACTIVE
        job.setStatus(Status.ACTIVE);

        // Company relationship
        job.setCompany(company);

        // Skills - Create JobSkill relationships
        if (skills != null && !skills.isEmpty()) {
            for (Skill skill : skills) {
                JobSkill jobSkill = new JobSkill();
                jobSkill.setJob(job);
                jobSkill.setSkill(skill);
                job.addSkill(jobSkill);
            }
        }

        return job;
    }

    /**
     * Convert từ Job Entity sang JobResponse DTO
     * Sử dụng khi trả về data cho client
     *
     * @param job Job entity từ database
     * @return JobResponse DTO cho UI
     */
    public JobResponse toResponse(Job job) {
        if (job == null) {
            return null;
        }

        // Convert skills
        List<JobResponse.SkillLookupResponse> skillResponses = Collections.emptyList();
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            skillResponses = job.getRequiredSkills().stream()
                    .map(jobSkill -> JobResponse.SkillLookupResponse.builder()
                            .id(jobSkill.getSkill().getId())
                            .name(jobSkill.getSkill().getName())
                            .build())
                    .collect(Collectors.toList());
        }

        return JobResponse.builder()
                // Basic Info
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .department(job.getDepartment())

                // Arrays
                .responsibilities(job.getResponsibilities())
                .qualifications(job.getQualifications())
                .minimumQualifications(job.getMinimumQualifications())
                .benefits(job.getBenefits())

                // Experience
                .minExperience(job.getMinExperience())
                .maxExperience(job.getMaxExperience())
                .experienceLevel(job.getExperienceLevel())

                // Job Type & Category
                .employmentType(job.getEmploymentType())
                .type(job.getEmploymentType()) // UI dùng 'type' thay vì 'employmentType'
                .jobCategory(job.getJobCategory())
                .jobFunction(job.getJobCategory()) // UI dùng 'jobFunction' thay vì 'jobCategory'
                .education(job.getEducation())

                // Location
                .state(job.getState())
                .city(job.getCity())
                .district(job.getDistrict())
                .specific(job.getAddress()) // Map 'address' -> 'specific' cho UI
                .remoteJob(job.isRemoteJob())

                // Skills
                .skills(skillResponses)

                // Compensation & Contact
                .salaryRange(job.getSalaryRange())
                .contactEmail(job.getContactEmail())
                .contactPhone(job.getContactPhone())

                // Posting Info
                .postedDate(job.getPostedDate())
                .expiryDate(job.getExpiryDate())
                .numberOfPositions(job.getNumberOfPositions())
                .promotionType(job.getPromotionType())
                .status(job.getStatus())

                // Stats
                .views(job.getViews())
                .applicants(job.getApplicants())
                .saved(job.getSaved())
                .likes(job.getLiked())
                .shares(job.getShared())

                // Timeline - null for now, có thể implement sau
                .timeline(null)

                .build();
    }

    /**
     * Convert list of Job entities sang list JobResponse
     */
    public List<JobResponse> toResponseList(List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }

        return jobs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
