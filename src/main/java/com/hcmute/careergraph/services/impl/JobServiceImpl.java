package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.EducationType;
import com.hcmute.careergraph.enums.job.EmploymentType;
import com.hcmute.careergraph.enums.job.ExperienceLevel;
import com.hcmute.careergraph.enums.job.JobCategory;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.helper.VietnamProvinceUtils;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobRecruimentRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobSettingsUpdateRequest;
import com.hcmute.careergraph.persistence.dtos.response.CvSuggestionResponse;
import com.hcmute.careergraph.persistence.event.JobCreatedEvent;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.CandidateEducation;
import com.hcmute.careergraph.persistence.models.CandidateExperience;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final CandidateRepository candidateRepository;
    private final JobMapper jobMapper;
    private final JobESService jobESService;
    private final ObjectMapper objectMapper;

    private final Integer PAGE_SIZE_PERSONAL_JOB = 8;
    private final EmbedService embedService;
    private final JobESRepository jobESRepository;
    private final FastAPIClientService fastAPIClientService;
    private final CandidateSearchTextBuilder candidateSearchTextBuilder;
    private final RedisService redisService;
    private final FileRepository fileRepository;
    private final CompanyAccessPolicyService companyAccessPolicyService;
    private final JobSearchDocumentFactory jobSearchDocumentFactory;

    private final ApplicationEventPublisher publisher;

    @Value("${application.cv-suggestion.max-uploaded-context-chars:24000}")
    private int maxUploadedCvContextChars;

    /**
     * Tạo job mới
     *
     * @param request   JobCreationRequest từ client
     * @param companyId ID của công ty đăng job (lấy từ authenticated user)
     * @return JobResponse chứa thông tin job vừa tạo
     * @throws NotFoundException nếu company không tồn tại
     */
    @Transactional
    @Override
    public Job createJob(JobCreationRequest request, String companyId) {
        log.info("Creating new job with title: {} for company ID: {}", request.title(), companyId);

        // 1. Validate và lấy Company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found with ID: " + companyId));
        companyAccessPolicyService.assertCompanyCanManageJobs(company);

        Job job = jobMapper.toEntity(request, company);
        // 4. Lưu vào database
        Job savedJob = jobRepository.save(job);
        syncJobSearchDocument(savedJob);
        log.info("Job created successfully with ID: {}", savedJob.getId());
        publisher.publishEvent(new JobCreatedEvent(savedJob.getId()));
        return savedJob;
    }

    private String toEnumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private LocalDate safeParseDate(String date) {
        try {
            return (date == null || date.isBlank())
                    ? null
                    : LocalDate.parse(date);
        } catch (Exception e) {
            return null; // ES cho phép null
        }
    }

    /**
     * Lấy thông tin chi tiết job theo ID
     *
     * @param jobId ID của job cần lấy
     * @return JobResponse
     * @throws IllegalArgumentException nếu job không tồn tại
     */
    @Override
    public Job getJobById(String jobId) {
        log.info("Fetching job with ID: {}", jobId);

        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));
    }

    @Transactional
    @Override
    public Job incrementJobViews(String jobId) {
        Job job = getJobById(jobId);
        job.setViews(job.getViews() + 1);
        return jobRepository.save(job);
    }

    /**
     * Lấy tất cả jobs của một company
     *
     * @param companyId ID của company
     * @return List JobResponse
     */
    @Transactional(readOnly = true)
    @Override
    public Page<Job> getJobsByCompany(String companyId, Pageable pageable) {
        log.info("Fetching all jobs for company ID: {}", companyId);

        Page<Job> jobs = jobRepository.findByCompanyId(companyId, pageable);
        return jobs;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Job> getPublicJobsByCompany(String companyId, Pageable pageable) {
        return jobRepository.findPublicJobsByCompanyId(
                companyId,
                CompanyVerificationStatus.APPROVED,
                CompanyOperationalStatus.ACTIVE,
                LocalDate.now().toString(),
                pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Job> getAllJobs(Pageable pageable) {
        log.info("Fetching all jobs");

        Page<Job> jobs = jobRepository.findPublicJobs(
                CompanyVerificationStatus.APPROVED,
                CompanyOperationalStatus.ACTIVE,
                LocalDate.now().toString(),
                pageable);
        return jobs;
    }

    /**
     * Update job
     */
    @Transactional
    @Override
    public Job updateJob(String jobId, JobCreationRequest request, String companyId) {
        log.info("Updating job with ID: {} for company ID: {}", jobId, companyId);

        Job existingJob = findOwnedJob(jobId, companyId);

        existingJob.setTitle(request.title());
        existingJob.setDescription(request.description());
        existingJob.setDepartment(request.department());
        existingJob.setResponsibilities(
                request.responsibilities() != null ? request.responsibilities() : Collections.emptyList());
        existingJob.setQualifications(
                request.qualifications() != null ? request.qualifications() : Collections.emptyList());
        existingJob.setMinimumQualifications(
                request.minimumQualifications() != null ? request.minimumQualifications() : Collections.emptyList());
        existingJob.setBenefits(request.benefits() != null ? request.benefits() : Collections.emptyList());
        existingJob.setMinExperience(request.minExperience());
        existingJob.setMaxExperience(request.maxExperience());
        existingJob.setExperienceLevel(request.experienceLevel());
        existingJob.setEmploymentType(request.employmentType());
        existingJob.setJobCategory(request.jobCategory());
        existingJob.setEducation(request.education());
        existingJob.setRemoteJob(Boolean.TRUE.equals(request.remoteJob()));
        existingJob.setState(request.state());
        existingJob.setCity(request.city());
        existingJob.setDistrict(request.district());
        existingJob.setAddress(request.address());
        existingJob.setSalaryRange(request.salaryRange());
        existingJob.setContactEmail(request.contactEmail());
        existingJob.setContactPhone(request.contactPhone());
        existingJob.setPromotionType(request.promotionType() != null ? request.promotionType() : "free");
        existingJob.setNumberOfPositions(request.numberOfPositions() != null ? request.numberOfPositions() : 1);
        existingJob.setExpiryDate(request.expiryDate());

        Job savedJob = jobRepository.save(existingJob);
        syncJobSearchDocument(savedJob);
        return savedJob;
    }

    @Override
    public Job updateJob(String jobId, String companyId, JobRecruimentRequest request) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found with ID: " + jobId));

        if (!job.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Company not have job with ID: " + jobId);
        }

        // Update recruiment for job
        job.setResume(request.resume());
        job.setCoverLetter(request.coverLetter());

        return jobRepository.save(job);
    }

    /**
     * Publish job
     *
     * @param jobId:     ID of job
     * @param companyId: ID of company
     * @return Job entity
     */
    @Override
    public Job publishJob(String jobId, String companyId) {
        log.info("Publish job with ID: {}", jobId);

        if (jobId == null || companyId == null) {
            throw new BadRequestException("JobID or CompanyID is not null");
        }

        Optional<Job> job = jobRepository.findByIdAndCompanyId(jobId, companyId);
        if (job.isEmpty()) {
            throw new NotFoundException("Job not found with ID: " + jobId);
        }

        companyAccessPolicyService.assertCompanyCanManageJobs(job.get().getCompany());
        // Update job
        job.get().setStatus(Status.ACTIVE);
        Job savedJob = jobRepository.save(job.get());
        syncJobSearchDocument(savedJob);

        return savedJob;
    }

    /**
     * Delete job (soft delete - chuyển status sang CLOSED)
     */
    @Transactional
    @Override
    public void deleteJob(String jobId, String companyId) {
        log.info("Deleting job with ID: {} for company ID: {}", jobId, companyId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        // Validate job thuộc về company
        if (!job.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Job does not belong to this company");
        }

        // Soft delete: chuyển status sang CLOSED
        job.setStatus(Status.CLOSED);
        Job savedJob = jobRepository.save(job);
        syncJobSearchDocument(savedJob);

        log.info("Job deleted successfully with ID: {}", jobId);
    }

    @Override
    @Transactional
    public Job updateJobSettings(String jobId, String companyId, JobSettingsUpdateRequest request) {
        if (request == null || !request.hasUpdates()) {
            throw new BadRequestException("At least one job setting must be provided");
        }

        Job job = findOwnedJob(jobId, companyId);

        if (request.aiScreeningEnabled() != null) {
            job.setAiScreeningEnabled(request.aiScreeningEnabled());
        }

        if (StringUtils.hasText(request.expiryDate())) {
            LocalDate parsedExpiryDate = parseExpiryDate(request.expiryDate());
            job.setExpiryDate(parsedExpiryDate.toString());
        }

        if (request.status() != null) {
            if (request.status() == Status.ACTIVE) {
                companyAccessPolicyService.assertCompanyCanManageJobs(job.getCompany());
            }
            applyManagedStatus(job, request.status());
        }

        Job savedJob = jobRepository.save(job);
        syncJobSearchDocument(savedJob);
        return savedJob;
    }

    @Transactional
    @Override
    public void activateJob(String jobId, String companyId) {
        Job job = findOwnedJob(jobId, companyId);
        companyAccessPolicyService.assertCompanyCanManageJobs(job.getCompany());
        applyManagedStatus(job, Status.ACTIVE);
        Job savedJob = jobRepository.save(job);
        syncJobSearchDocument(savedJob);
    }

    @Transactional
    @Override
    public void deactivateJob(String jobId, String companyId) {
        Job job = findOwnedJob(jobId, companyId);
        applyManagedStatus(job, Status.INACTIVE);
        Job savedJob = jobRepository.save(job);
        syncJobSearchDocument(savedJob);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Job> getJobByCategory(JobCategory jobCategory, Pageable pageable) {

        if (jobCategory == null) {
            return Page.empty();
        }

        String currentDate = LocalDate.now().toString();
        return jobRepository.findByJobCategory(jobCategory,
                CompanyVerificationStatus.APPROVED, CompanyOperationalStatus.ACTIVE, currentDate, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Job> getJobsPersonalized(String userId) {

        // Validate userId
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        // Check if candidate exists
        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Candidate not found with id: " + userId));

        // Get current date for filtering expired jobs
        String currentDate = LocalDate.now().toString();

        // Fetch and return personalized jobs from repository
        List<Job> personalJobs = jobRepository.findJobByPersonalized(userId, currentDate).stream()
                .filter(this::isJobPubliclyAvailable)
                .toList();
        if (personalJobs.size() >= PAGE_SIZE_PERSONAL_JOB) {
            return personalJobs.subList(0, PAGE_SIZE_PERSONAL_JOB);
        }

        int remaining = PAGE_SIZE_PERSONAL_JOB - personalJobs.size();
        List<String> excludeIds = personalJobs.stream().map(Job::getId).toList();
        List<Job> extraJobs = jobRepository.findLatestJobsExcluding(currentDate,
                excludeIds.isEmpty() ? null : excludeIds,
                CompanyVerificationStatus.APPROVED, CompanyOperationalStatus.ACTIVE).stream()
                .limit(remaining)
                .toList();

        List<Job> result = new ArrayList<>(personalJobs);
        result.addAll(extraJobs);

        return result;
    }

    private String buildStructuredCandidateContext(Candidate candidate) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== EXPERIENCE (DB) ===\n");
        if (candidate.getExperiences() != null && !candidate.getExperiences().isEmpty()) {
            candidate.getExperiences().forEach(exp -> {
                sb.append("{\n");
                sb.append("  id: ").append(exp.getId()).append("\n");
                sb.append("  jobTitle: ").append(safe(exp.getJobTitle())).append("\n");
                sb.append("  companyName: ").append(safe(exp.getCompany() != null ? exp.getCompany().getName() : ""))
                        .append("\n");
                sb.append("  startDate: ").append(safe(exp.getStartDate())).append("\n");
                sb.append("  endDate: ").append(safe(exp.getEndDate())).append("\n");
                sb.append("  isCurrent: ").append(exp.getIsCurrent()).append("\n");
                sb.append("  description: ").append(safe(exp.getDescription())).append("\n");
                sb.append("}\n");
            });
        } else {
            sb.append("(No experiences in DB)\n");
        }

        sb.append("\n=== EDUCATION (DB) ===\n");
        if (candidate.getEducations() != null && !candidate.getEducations().isEmpty()) {
            candidate.getEducations().forEach(edu -> {
                sb.append("{\n");
                sb.append("  id: ").append(edu.getId()).append("\n");
                sb.append("  schoolName: ")
                        .append(safe(edu.getEducation() != null ? edu.getEducation().getOfficialName() : ""))
                        .append("\n");
                sb.append("  degreeTitle: ").append(safe(edu.getDegreeTitle())).append("\n");
                sb.append("  major: ").append(safe(edu.getMajor())).append("\n");
                sb.append("  startDate: ").append(safe(edu.getStartDate())).append("\n");
                sb.append("  endDate: ").append(safe(edu.getEndDate())).append("\n");
                sb.append("  isCurrent: ").append(edu.getIsCurrent()).append("\n");
                sb.append("  description: ").append(safe(edu.getDescription())).append("\n");
                sb.append("}\n");
            });
        } else {
            sb.append("(No educations in DB)\n");
        }

        sb.append("\n=== SKILLS (DB) ===\n");
        if (candidate.getSkills() != null && !candidate.getSkills().isEmpty()) {
            candidate.getSkills().forEach(skill -> {
                if (skill.getSkill() != null) {
                    sb.append("- ").append(skill.getSkill().getName()).append("\n");
                }
            });
        } else {
            sb.append("(No skills in DB)\n");
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Job> getJobsPersonalizedES(String userId) {

        // Validate userId
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        // Check if candidate exists
        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Candidate not found with id: " + userId));

        // V2: Use structured profile instead of raw text dump
        var profile = candidateSearchTextBuilder.buildProfile(candidate);
        // Prefer intentText for BM25 (high signal), fallback to cvKeywords
        String keyword = profile.getIntentText();
        if (!StringUtils.hasText(keyword)) {
            keyword = profile.getCvKeywords();
        } else if (StringUtils.hasText(profile.getCvKeywords())) {
            // Append cvKeywords as boost signal (but intent leads)
            keyword = keyword + " " + profile.getCvKeywords();
        }

        if (!StringUtils.hasText(keyword)) {
            return getJobsForAnonymousUser();
        }
        log.debug("Personalized job search V2 for candidateId={}, intent={}, cvSrc={}, chars={}",
                userId, profile.isHasIntent(), profile.getCvKeywordsSource(), keyword.length());
        // Get current date for filtering expired jobs
        Pageable pageable = PageRequest.of(0, 6);

        SearchResponse<JobES> listSearch = jobESService.searchJobsByNavtiveAndFuzzy(keyword, pageable);
        if (listSearch == null || listSearch.hits() == null || listSearch.hits().hits().isEmpty()) {
            log.warn(
                    "Personalized ES search returned no usable response for candidateId={}, falling back to latest jobs",
                    userId);
            return getJobsForAnonymousUser();
        }

        List<String> esIds = listSearch.hits().hits().stream()
                .map(Hit::id)
                .toList();
        return jobRepository.findAllById(esIds)
                .stream()
                .filter(this::isJobPubliclyAvailable)
                .sorted(Comparator.comparingInt(p -> esIds.indexOf(p.getId())))
                .toList();

    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String _genKey(Candidate candidate) {
        return candidateSearchTextBuilder.build(candidate, true);
    }

    private String buildJobSearchText(Job job) {
        if (job == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        append(sb, job.getTitle());
        append(sb, job.getDescription());
        append(sb, job.getJobCategory() != null ? job.getJobCategory().getDisplayName() : null);
        append(sb, job.getEmploymentType() != null ? job.getEmploymentType().name() : null);
        append(sb, job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null);
        append(sb, job.getEducation() != null ? job.getEducation().name() : null);
        append(sb, job.getCity());
        append(sb, job.getState());
        appendAll(sb, job.getQualifications());
        appendAll(sb, job.getMinimumQualifications());
        appendAll(sb, job.getResponsibilities());
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private void appendAll(StringBuilder sb, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> append(sb, value));
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value.trim()).append(' ');
        }
    }

    @Override
    public List<Job> getJobsForAnonymousUser() {
        String currentDate = LocalDate.now().toString();

        // Get new job
        List<Job> newJobs = jobRepository.findLatestActiveJobs(currentDate,
                CompanyVerificationStatus.APPROVED, CompanyOperationalStatus.ACTIVE, PageRequest.of(0, 8));
        if (newJobs == null) {
            return new ArrayList<>();
        }

        return newJobs.stream().filter(this::isJobPubliclyAvailable).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Job> getJobsPopular() {
        String currentDate = LocalDate.now().toString();

        List<Job> jobsPopular = jobRepository.findPopularJob(
                CompanyVerificationStatus.APPROVED, CompanyOperationalStatus.ACTIVE, currentDate);
        if (jobsPopular.isEmpty()) {
            return new ArrayList<>();
        }

        return jobsPopular.stream().filter(this::isJobPubliclyAvailable).toList();
    }

    @Override
    public Page<Job> getSimilarJob(String jobId, Pageable pageable) {
        String currentDate = LocalDate.now().toString();

        Page<Job> jobsSimilar = jobRepository.findSimilarJob(jobId,
                CompanyVerificationStatus.APPROVED, CompanyOperationalStatus.ACTIVE, currentDate, pageable);
        if (jobsSimilar == null) {
            return new PageImpl<>(null);
        }

        return filterPublicPage(jobsSimilar, pageable);
    }

    /**
     * Hàm lấy ra job theo query và company ID
     *
     * @param query     Dữ liệu tìm kiếm
     * @param companyId ID của company
     * @return Map<ID, Job>
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, String> lookup(String companyId, String query) {

        Map<String, String> jobs = jobRepository.lookup(companyId, query);

        if (jobs == null) {
            return Map.of();
        }

        return jobs;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Job> search(JobFilterRequest filter, String partyId, String query, Pageable pageable, PartyType type) {

        // Check company ID
        if (type == PartyType.COMPANY && partyId == null) {
            throw new BadRequestException("Company ID is required");
        }

        String currentDate = LocalDate.now().toString();

        // Get params from filter
        List<Status> statuses = filter.getStatuses() == null || filter.getStatuses().isEmpty() ? null
                : filter.getStatuses();
        List<JobCategory> jobCategories = filter.getJobCategories() == null || filter.getJobCategories().isEmpty()
                ? null
                : filter.getJobCategories();
        List<EmploymentType> employmentTypes = filter.getEmploymentTypes() == null
                || filter.getEmploymentTypes().isEmpty() ? null : filter.getEmploymentTypes();
        List<EducationType> educationTypes = null;
        List<ExperienceLevel> experienceLevels = null;
        String location = filter.getLocation();

        Page<Job> jobs = null;

        if (type == PartyType.COMPANY) {
            jobs = jobRepository.searchJobForCompany(partyId, statuses, jobCategories, employmentTypes, query,
                    currentDate, pageable);
        } else {

            /**
             * 1. API Search nhận input: keyword + filters
             * 2. Ghi vào bảng candidate_search_history
             * Tạo embedding → lưu vào cột embedding
             * 3. Khi trả kết quả:
             * Personalization service lấy lịch sử để:
             * Re-rank job
             * Suggest keyword
             * Recommend job trên landing page
             */

            jobs = jobRepository.searchJobForCandidate(partyId, location, jobCategories, employmentTypes,
                    experienceLevels, educationTypes, query,
                    CompanyVerificationStatus.APPROVED, CompanyOperationalStatus.ACTIVE, LocalDate.now().toString(),
                    pageable);
        }
        return jobs;
    }

    @Override
    public Page<Job> searchEmbed(JobFilterRequest filter, String partyId, String query, Pageable pageable,
            PartyType type) {
        // Check company ID
        if (type == PartyType.COMPANY && partyId == null) {
            throw new BadRequestException("Company ID is required");
        }

        String currentDate = LocalDate.now().toString();

        // Get params from filter
        List<Status> statuses = filter.getStatuses() == null || filter.getStatuses().isEmpty() ? null
                : filter.getStatuses();
        List<JobCategory> jobCategories = filter.getJobCategories() == null || filter.getJobCategories().isEmpty()
                ? null
                : filter.getJobCategories();
        List<EmploymentType> employmentTypes = filter.getEmploymentTypes() == null
                || filter.getEmploymentTypes().isEmpty() ? null : filter.getEmploymentTypes();
        List<EducationType> educationTypes = filter.getEducationTypes() == null || filter.getEducationTypes().isEmpty()
                ? null
                : filter.getEducationTypes();
        List<ExperienceLevel> experienceLevels = filter.getExperienceLevels() == null
                || filter.getExperienceLevels().isEmpty() ? null : filter.getExperienceLevels();
        String location = filter.getLocation();

        Page<Job> jobs = null;

        if (type == PartyType.COMPANY) {
            jobs = jobRepository.searchJobForCompany(partyId, statuses, jobCategories, employmentTypes, query,
                    currentDate, pageable);
        } else {
            String keyword = "";

            // Nếu không có keyword → dùng filter-only search (match_all + post filter)
            boolean hasKeyword = query != null && !query.trim().isEmpty();

            if (partyId != null) {
                Candidate candidate = candidateRepository.findById(partyId)
                        .orElse(null);
                if (candidate != null && !hasKeyword) {
                    // V2: Use embeddingText from structured profile for KNN search
                    var profile = candidateSearchTextBuilder.buildProfile(candidate);
                    keyword = profile.getEmbeddingText();
                }
            }

            if (hasKeyword) {
                keyword = query.trim();
            }

            SearchResponse<JobES> esResponse;

            if (keyword.trim().isEmpty()) {
                // Không có keyword và không có candidate profile → filter-only search
                esResponse = jobESService.filterOnlySearch(filter, partyId, pageable, type);
            } else {
                // Có keyword → hybrid search (KNN + BM25)
                esResponse = jobESService.knnSearch(keyword, filter, partyId, pageable, type);
            }

            if (esResponse == null || esResponse.hits().hits().isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            List<String> ids = esResponse.hits().hits()
                    .stream()
                    .map(Hit::id)
                    .toList();

            List<Job> ljobs = jobRepository.findAllById(ids)
                    .stream()
                    .filter(this::isJobPubliclyAvailable)
                    .sorted(Comparator.comparingInt(j -> ids.indexOf(j.getId())))
                    .toList();

            assert esResponse.hits().total() != null;
            long total = esResponse.hits().total().value();

            return new PageImpl<>(ljobs, pageable, total);
        }
        return jobs;
    }

    @Transactional(readOnly = true)
    @Override
    public CvSuggestionResponse generateCv(String jobId, String candidateId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (isJobExpired(job)) {
            throw new BadRequestException("Không thể tạo gợi ý CV cho công việc đã hết hạn ứng tuyển.");
        }

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        // Ensure experiences/educations/skills are loaded (Hibernate.initialize
        // equivalent via eager loading)
        if (candidate.getExperiences() != null) {
            candidate.getExperiences().size();
        }
        if (candidate.getEducations() != null) {
            candidate.getEducations().size();
        }
        if (candidate.getSkills() != null) {
            candidate.getSkills().size();
        }

        validateCvSuggestionLimit(candidateId);

        CvSourceContext cvSourceContext = buildCvSourceContext(candidateId, candidate);
        log.info("Generating CV suggestion for candidateId={} jobId={} source={} uploadedCvCount={} chars={}",
                candidateId, jobId, cvSourceContext.source(), cvSourceContext.uploadedCvCount(),
                cvSourceContext.text().length());

        String suggestionId = java.util.UUID.randomUUID().toString();
        try {
            String prompt = buildCvGenerationPrompt(job, cvSourceContext.text(), candidate);
            String jsonResponse = fastAPIClientService.cvSuggestion(prompt);
            String cleanJson = cleanJsonString(jsonResponse);
            CvSuggestionResponse aiResponse = objectMapper.readValue(cleanJson, CvSuggestionResponse.class);

            // Phase 1: Post-process AI response to ensure only DB data is used
            CvSuggestionResponse response = mergeAiCvSuggestion(aiResponse, candidate);
            response.setSuggestionId(suggestionId);

            // Store in Redis with 24h TTL
            redisService.setObject("cv_suggestion:" + suggestionId + ":" + candidateId, response, 86400);

            return response;
        } catch (Exception e) {
            log.error("Error generating CV suggestion, returning profile fallback for job context", e);
            CvSuggestionResponse fallback = buildFallbackCvSuggestionForJob(candidate, job);
            fallback.setSuggestionId(suggestionId);
            redisService.setObject("cv_suggestion:" + suggestionId + ":" + candidateId, fallback, 86400);
            return fallback;
        }
    }

    @Override
    @Transactional
    public void syncCompanyJobsSearchDocuments(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            return;
        }
        List<Job> companyJobs = jobRepository.findByCompanyId(companyId, Pageable.unpaged()).getContent();
        long publicJobs = companyJobs.stream().filter(jobSearchDocumentFactory::shouldIndex).count();
        long nonPublicJobs = companyJobs.size() - publicJobs;

        log.info("Syncing company jobs to Elasticsearch: companyId={}, totalJobs={}, publicJobs={}, nonPublicJobs={}",
                companyId, companyJobs.size(), publicJobs, nonPublicJobs);

        companyJobs.forEach(this::syncJobSearchDocument);
    }

    @Override
    public boolean isJobPubliclyAvailable(Job job) {
        return companyAccessPolicyService.isJobPubliclyAvailable(job);
    }

    public CvSuggestionResponse getCvSuggestion(String suggestionId, String candidateId) {
        String key = "cv_suggestion:" + suggestionId + ":" + candidateId;
        CvSuggestionResponse cached = redisService.getObject(key, CvSuggestionResponse.class);

        if (cached == null) {
            throw new ResourceNotFoundException(
                    "CV suggestion not found or has expired. suggestionId: " + suggestionId);
        }

        return cached;
    }

    // Helper để làm sạch chuỗi JSON nếu AI trả về dạng Markdown
    private void validateCvSuggestionLimit(String candidateId) {
        String key = "cv_suggestion_limit:" + candidateId;
        Integer current = redisService.getObject(key, Integer.class);
        if (current != null && current >= 100) {
            throw new BadRequestException("Bạn đã vượt quá số lần tạo gợi ý CV cho phép trong ngày (tối đa 10 lần).");
        }
        redisService.setObject(key, current == null ? 1 : current + 1, 86400);
    }

    private String buildCandidateProfileText(Candidate candidate) {
        StringBuilder sb = new StringBuilder();
        append(sb, fullName(candidate));
        append(sb, candidate.getCurrentJobTitle());
        append(sb, candidate.getDesiredPosition());
        append(sb, candidate.getSummary());
        append(sb, candidate.getIndustry());
        appendAll(sb, candidate.getIndustries());
        appendAll(sb, candidate.getLocations());
        appendAll(sb, candidate.getWorkTypes());

        if (candidate.getSkills() != null) {
            candidate.getSkills().stream()
                    .map(skill -> skill.getSkill() != null ? skill.getSkill().getName() : null)
                    .forEach(value -> append(sb, value));
        }
        if (candidate.getExperiences() != null) {
            candidate.getExperiences().forEach(experience -> {
                append(sb, experience.getJobTitle());
                append(sb, experience.getDescription());
                if (experience.getCompany() != null) {
                    append(sb, experience.getCompany().getName());
                }
            });
        }
        if (candidate.getEducations() != null) {
            candidate.getEducations().forEach(education -> {
                append(sb, education.getDegreeTitle());
                append(sb, education.getMajor());
                if (education.getEducation() != null) {
                    append(sb, education.getEducation().getOfficialName());
                }
            });
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private CvSourceContext buildCvSourceContext(String candidateId, Candidate candidate) {
        List<File> uploadedResumes = fileRepository.findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                candidateId, Status.ACTIVE, List.of(FileType.RESUME, FileType.CV));

        if (uploadedResumes == null || uploadedResumes.isEmpty()) {
            return CvSourceContext.profile(buildCandidateProfileText(candidate));
        }

        StringBuilder sb = new StringBuilder();
        Set<String> seenHashes = new HashSet<>();
        int includedCount = 0;

        for (File resume : uploadedResumes) {
            String resumeText = normalizeResumeText(resume.getResumeExtractedText());
            if (!StringUtils.hasText(resumeText)) {
                continue;
            }

            String dedupeKey = StringUtils.hasText(resume.getResumeContentHash())
                    ? resume.getResumeContentHash()
                    : Integer.toHexString(resumeText.hashCode());
            if (!seenHashes.add(dedupeKey)) {
                continue;
            }

            String block = buildResumeContextHeader(resume, includedCount + 1) + "\n" + resumeText + "\n\n";
            int remaining = maxUploadedCvContextChars - sb.length();
            if (remaining <= 0) {
                break;
            }

            if (block.length() <= remaining) {
                sb.append(block);
            } else {
                sb.append(block, 0, remaining);
            }

            includedCount++;
            if (sb.length() >= maxUploadedCvContextChars) {
                break;
            }
        }

        if (!StringUtils.hasText(sb.toString())) {
            return CvSourceContext.profile(buildCandidateProfileText(candidate));
        }

        return CvSourceContext.uploaded(sb.toString().trim(), includedCount);
    }

    private String buildResumeContextHeader(File resume, int index) {
        String fileName = firstText(resume.getFileName(), resume.getOriginalFileName(), "uploaded_cv_" + index);
        String uploadedAt = resume.getCreatedDate() != null ? resume.getCreatedDate().toString() : "unknown_time";
        return "[UPLOADED_CV_" + index + " | " + fileName + " | created_at=" + uploadedAt + "]";
    }

    private String normalizeResumeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.replace('\u0000', ' ').trim();
    }

    private CvSuggestionResponse buildFallbackCvSuggestion(Candidate candidate) {
        List<CvSuggestionResponse.Experience> allExperiences = fallbackExperiences(candidate);

        return CvSuggestionResponse.builder()
                .personal(CvSuggestionResponse.PersonalInfo.builder()
                        .fullName(fullName(candidate))
                        .headline(firstText(candidate.getCurrentJobTitle(), candidate.getDesiredPosition()))
                        .summary(candidate.getSummary())
                        .location(candidateLocation(candidate))
                        .build())
                .contact(CvSuggestionResponse.ContactInfo.builder()
                        .email(candidateContact(candidate, ContactType.EMAIL))
                        .phone(candidateContact(candidate, ContactType.PHONE))
                        .build())
                .experience(allExperiences)
                .education(fallbackEducations(candidate))
                .skills(fallbackSkills(candidate))
                .matchedSkills(Collections.emptyList())
                .missingSkills(Collections.emptyList())
                .suggestions(
                        List.of("AI service is temporarily unavailable. Please review and tailor this CV manually."))
                .overallMatchScore(0)
                .allExperiences(allExperiences)
                .build();
    }

    private CvSuggestionResponse buildFallbackCvSuggestionForJob(Candidate candidate, Job job) {
        List<CvSuggestionResponse.Experience> allExperiences = filterExperiencesByJobContext(candidate, job);

        return CvSuggestionResponse.builder()
                .personal(CvSuggestionResponse.PersonalInfo.builder()
                        .fullName(fullName(candidate))
                        .headline(job.getTitle() != null ? job.getTitle()
                                : firstText(candidate.getCurrentJobTitle(), candidate.getDesiredPosition()))
                        .summary(candidate.getSummary())
                        .location(candidateLocation(candidate))
                        .build())
                .contact(CvSuggestionResponse.ContactInfo.builder()
                        .email(candidateContact(candidate, ContactType.EMAIL))
                        .phone(candidateContact(candidate, ContactType.PHONE))
                        .build())
                .experience(allExperiences)
                .education(fallbackEducations(candidate))
                .skills(fallbackSkills(candidate))
                .matchedSkills(Collections.emptyList())
                .missingSkills(Collections.emptyList())
                .suggestions(
                        List.of("AI service is temporarily unavailable. Please review and tailor this CV manually."))
                .overallMatchScore(0)
                .allExperiences(allExperiences)
                .build();
    }

    private List<CvSuggestionResponse.Experience> fallbackExperiences(Candidate candidate) {
        if (candidate.getExperiences() == null) {
            return Collections.emptyList();
        }
        return candidate.getExperiences().stream()
                .map(experience -> CvSuggestionResponse.Experience.builder()
                        .id(experience.getId())
                        .role(experience.getJobTitle())
                        .company(experience.getCompany() != null ? experience.getCompany().getName() : null)
                        .location("")
                        .startDate(experience.getStartDate())
                        .endDate(experience.getEndDate())
                        .bulletPoints(StringUtils.hasText(experience.getDescription())
                                ? List.of(experience.getDescription())
                                : Collections.emptyList())
                        .build())
                .toList();
    }

    private List<CvSuggestionResponse.Experience> filterExperiencesByJobContext(Candidate candidate, Job job) {
        if (candidate.getExperiences() == null || candidate.getExperiences().isEmpty()) {
            return Collections.emptyList();
        }

        String jobTitle = StringUtils.hasText(job.getTitle()) ? job.getTitle().toLowerCase() : "";
        String jobDescription = StringUtils.hasText(job.getDescription()) ? job.getDescription().toLowerCase() : "";
        String searchKeywords = jobTitle + " " + jobDescription;

        return candidate.getExperiences().stream()
                .filter(experience -> {
                    String expTitle = StringUtils.hasText(experience.getJobTitle())
                            ? experience.getJobTitle().toLowerCase()
                            : "";
                    String expDescription = StringUtils.hasText(experience.getDescription())
                            ? experience.getDescription().toLowerCase()
                            : "";

                    boolean titleMatches = expTitle.length() > 0 && isRelevantMatch(jobTitle, expTitle);
                    boolean keywordMatches = searchKeywords.length() > 0
                            && isRelevantMatch(searchKeywords, expDescription);

                    return titleMatches || keywordMatches;
                })
                .map(experience -> CvSuggestionResponse.Experience.builder()
                        .id(experience.getId())
                        .role(experience.getJobTitle())
                        .company(experience.getCompany() != null ? experience.getCompany().getName() : null)
                        .location("")
                        .startDate(experience.getStartDate())
                        .endDate(experience.getEndDate())
                        .bulletPoints(StringUtils.hasText(experience.getDescription())
                                ? List.of(experience.getDescription())
                                : Collections.emptyList())
                        .build())
                .toList();
    }

    private boolean isRelevantMatch(String keywords, String text) {
        if (!StringUtils.hasText(keywords) || !StringUtils.hasText(text)) {
            return false;
        }
        String[] keywordArray = keywords.split("\\s+");
        for (String keyword : keywordArray) {
            if (keyword.length() > 2 && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<CvSuggestionResponse.Education> fallbackEducations(Candidate candidate) {
        if (candidate.getEducations() == null) {
            return Collections.emptyList();
        }
        return candidate.getEducations().stream()
                .map(education -> CvSuggestionResponse.Education.builder()
                        .id(education.getId())
                        .school(education.getEducation() != null ? education.getEducation().getOfficialName() : null)
                        .degree(firstText(education.getDegreeTitle(), education.getMajor()))
                        .startDate(education.getStartDate())
                        .endDate(education.getEndDate())
                        .build())
                .toList();
    }

    private List<CvSuggestionResponse.Skill> fallbackSkills(Candidate candidate) {
        if (candidate.getSkills() == null) {
            return Collections.emptyList();
        }
        return candidate.getSkills().stream()
                .filter(skill -> skill.getSkill() != null)
                .map(skill -> CvSuggestionResponse.Skill.builder()
                        .id(skill.getSkill().getId())
                        .name(skill.getSkill().getName())
                        .build())
                .toList();
    }

    private String constrainHeadlineLength(String headline) {
        int minLength = 40;
        int maxLength = 120;

        if (!StringUtils.hasText(headline)) {
            return headline;
        }

        String trimmed = headline.trim();

        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength).trim();
            if (!trimmed.endsWith(".") && !trimmed.endsWith("...")) {
                trimmed = trimmed + "...";
            }
        }

        return trimmed;
    }

    private boolean isJobExpired(Job job) {
        if (job == null) {
            return false;
        }

        if (job.getStatus() != null && (job.getStatus() == Status.EXPIRED ||
                job.getStatus() == Status.CLOSED ||
                job.getStatus() == Status.CANCELED ||
                job.getStatus() == Status.INACTIVE ||
                job.getStatus() == Status.SUSPENDED ||
                job.getStatus() == Status.DELETED ||
                job.getStatus() == Status.ARCHIVED ||
                job.getStatus() == Status.STOPPED)) {
            return true;
        }

        if (job.getExpiryDate() != null && !job.getExpiryDate().isEmpty()) {
            try {
                LocalDate expiryDate = LocalDate.parse(job.getExpiryDate());
                return expiryDate.isBefore(LocalDate.now());
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private String detectLanguage(Job job) {
        if (job == null) {
            return "Vietnamese";
        }

        String textToAnalyze = (job.getTitle() != null ? job.getTitle() : "")
                + " " + (job.getDescription() != null ? job.getDescription() : "")
                + " " + (job.getQualifications() != null ? job.getQualifications() : "");

        if (!StringUtils.hasText(textToAnalyze)) {
            return "Vietnamese";
        }

        int vietnameseDiacriticCount = 0;
        int totalCharacterCount = textToAnalyze.length();

        for (char c : textToAnalyze.toCharArray()) {
            if ((c >= 'À' && c <= 'ÿ') || (c >= 'Ā' && c <= 'ſ') || (c >= 'Ḁ' && c <= 'ỿ')) {
                vietnameseDiacriticCount++;
            }
        }

        double vietneseseRatio = (double) vietnameseDiacriticCount / totalCharacterCount;

        if (vietneseseRatio > 0.1) {
            return "Vietnamese";
        } else {
            return "English";
        }
    }

    private String fullName(Candidate candidate) {
        return (safe(candidate.getFirstName()) + " " + safe(candidate.getLastName())).trim();
    }

    private String candidateContact(Candidate candidate, ContactType type) {
        if (candidate.getContacts() == null) {
            return null;
        }
        return candidate.getContacts().stream()
                .filter(contact -> contact.getContactType() == type)
                .sorted(Comparator.comparing(contact -> !Boolean.TRUE.equals(contact.getIsPrimary())))
                .map(contact -> safe(contact.getValue()))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String candidateLocation(Candidate candidate) {
        if (candidate.getAddresses() == null) {
            return firstText(candidate.getWorkLocation(), joinList(candidate.getLocations()));
        }
        String address = candidate.getAddresses().stream()
                .sorted(Comparator.comparing(addressItem -> !Boolean.TRUE.equals(addressItem.getIsPrimary())))
                .map(addressItem -> joinList(
                        List.of(addressItem.getProvince(), addressItem.getDistrict(), addressItem.getCountry())))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        return firstText(candidate.getWorkLocation(), address, joinList(candidate.getLocations()));
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String cleanJsonString(String response) {
        if (response.contains("```json")) {
            return response.replace("```json", "").replace("```", "").trim();
        }
        if (response.contains("```")) {
            return response.replace("```", "").trim();
        }
        return response;
    }

    private String buildCvGenerationPrompt(Job job, String cvText, Candidate candidate) {
        StringBuilder sb = new StringBuilder();

        String detectedLanguage = detectLanguage(job);

        if ("English".equals(detectedLanguage)) {
            sb.append("Act as a professional career counselor and CV writing expert (Top CV Writer). ");
            sb.append(
                    "Your task is to rewrite the candidate's CV content to be the best fit for the job being applied for.\n\n");
        } else {
            sb.append("Đóng vai là một chuyên gia tư vấn nghề nghiệp và viết CV chuyên nghiệp (Top CV Writer). ");
            sb.append(
                    "Nhiệm vụ của bạn là viết lại nội dung CV cho ứng viên để phù hợp nhất với công việc đang ứng tuyển.\n\n");
        }

        // --- Input: Structured Candidate Context (DB as source of truth) ---
        sb.append("--- DỮ LIỆU CẤU TRÚC CỦA ỨNG VIÊN TỪ DB (NGUỒN SỰ THẬT) ---\n");
        sb.append(buildStructuredCandidateContext(candidate)).append("\n");

        // --- Input: Job Info ---
        sb.append("--- THÔNG TIN CÔNG VIỆC (TARGET JOB) ---\n");
        sb.append("Vị trí: ").append(job.getTitle() != null ? job.getTitle() : "N/A").append("\n");
        sb.append("Công ty: ").append(job.getCompany() != null ? job.getCompany().getName() : "N/A").append("\n");
        sb.append("Mô tả công việc: ").append(job.getDescription() != null ? job.getDescription() : "N/A").append("\n");
        sb.append("Yêu cầu kỹ năng: ").append(job.getQualifications() != null ? job.getQualifications() : "N/A")
                .append("\n\n");

        // --- Input: CV upload text (supplementary only) ---
        sb.append("--- TÀI LIỆU BỔ SUNG (CV UPLOAD - chỉ dùng để viết lại summary/headline/văn phong) ---\n");
        sb.append(
                "⚠️ QUAN TRỌNG: Tài liệu này CHỈ được dùng để cải thiện tông điệu, clarity, và content của summary/headline/bulletPoints.\n");
        sb.append("   KHÔNG được dùng để thêm hoặc tạo mới experience/education ngoài danh sách DB ở trên.\n");
        sb.append(StringUtils.hasText(cvText) ? cvText : "(Không có CV upload)\n").append("\n\n");

        // --- Output Requirement ---
        sb.append("--- YÊU CẦU ĐẦU RA ---\n");
        sb.append("NGUYÊN TẮC CỨNG (Hard Rules):\n");
        sb.append("1. experience[] CHỈ chứa các mục có trong EXPERIENCE (DB) ở trên.\n");
        sb.append("2. education[] CHỈ chứa các mục có trong EDUCATION (DB) ở trên.\n");
        sb.append("3. KHÔNG được tạo thêm mục kinh nghiệm/học vấn mới từ tài liệu CV upload.\n");
        sb.append("4. Hãy trả về kèm 'id' gốc từ DB cho mỗi experience/education để Java có thể reconcile.\n");
        sb.append("5. Chỉ viết lại bulletPoints, summary, headline sao cho phù hợp với job description.\n");
        sb.append("6. ĐÁnh giá mỗi experience: có liên quan đến job không? QUAN TRỌNG: 'relevant' = true CHỈ khi:\n");
        sb.append(
                "   - Job title/specialty của experience MATCH với target job title/specialty (VD: Front-end dev job → chỉ lấy front-end experiences)\n");
        sb.append("   - HOẶC experience chứa các skills/technologies được yêu cầu trong job requirements\n");
        sb.append(
                "   - Nếu experience là specialty KHÁC (VD: Backend, UX/UI, QA khi job là Frontend) → relevant = false\n");
        sb.append("   Trả về 'relevant' (boolean) và 'relevanceReason' (text ngắn giải thích lý do).\n");

        if ("English".equals(detectedLanguage)) {
            sb.append(
                    "7. EXPERIENCE RELEVANCE: Only include experiences where 'relevant' = true. CRITICAL: Mark 'relevant' = true ONLY when:\n");
            sb.append(
                    "   - Experience job title/specialty MATCHES the target job title/specialty (e.g., Frontend job → only Frontend experiences)\n");
            sb.append("   - OR the experience contains skills/technologies required in job qualifications\n");
            sb.append(
                    "   - If experience is DIFFERENT specialty (e.g., Backend, UX/UI, QA when job is Frontend) → relevant = false\n");
            sb.append("   Return 'relevanceReason' with brief explanation of why relevant or not.\n");
            sb.append(
                    "8. HEADLINE LENGTH: The 'headline' field MUST be between 40-120 characters. Keep it concise, professional, and impactful.\n");
            sb.append(
                    "9. LANGUAGE: The job description is in English. ALL generated content (headline, summary, bulletPoints, suggestions) MUST be in English ONLY. Do NOT mix Vietnamese and English.\n\n");
            sb.append("OUTPUT FORMAT: A single valid JSON (no markdown, no explanation):\n");
            sb.append("{\n" +
                    "  \"personal\": { \"fullName\": \"...\", \"headline\": \"...\", \"summary\": \"...\", \"location\": \"...\" },\n"
                    +
                    "  \"contact\": { \"email\": \"...\", \"phone\": \"...\", \"linkedin\": \"...\", \"website\": \"...\" },\n"
                    +
                    "  \"experience\": [\n" +
                    "    {\n" +
                    "      \"id\": \"<id from DB>\",\n" +
                    "      \"role\": \"...\",\n" +
                    "      \"company\": \"...\",\n" +
                    "      \"location\": \"...\",\n" +
                    "      \"startDate\": \"...\",\n" +
                    "      \"endDate\": \"...\",\n" +
                    "      \"bulletPoints\": [\"...\"],\n" +
                    "      \"relevant\": true/false,\n" +
                    "      \"relevanceReason\": \"<reason why this experience is relevant or not>\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"education\": [ { \"id\": \"<id from DB>\", \"school\": \"...\", \"degree\": \"...\", \"startDate\": \"...\", \"endDate\": \"...\" } ],\n"
                    +
                    "  \"skills\": [ { \"id\": \"<id from DB if available>\", \"name\": \"...\" } ],\n" +
                    "  \"languages\": [ { \"id\": \"<id>\", \"language\": \"...\", \"proficiency\": \"...\" } ],\n" +
                    "  \"awards\": [ { \"id\": \"<id>\", \"title\": \"...\", \"issuer\": \"...\", \"year\": \"...\" } ],\n"
                    +
                    "  \"matchedSkills\": [\"...\"],\n" +
                    "  \"missingSkills\": [\"...\"],\n" +
                    "  \"suggestions\": [\"...\"],\n" +
                    "  \"overallMatchScore\": <0-100>\n" +
                    "}");
        } else {
            sb.append(
                    "7. CHỈ LỌC EXPERIENCES LIÊN QUAN: Chỉ trả về những experiences có 'relevant' = true. KHÔNG trả về experiences không liên quan.\n");
            sb.append(
                    "8. HEADLINE LENGTH: Field 'headline' PHẢI có độ dài 40-120 ký tự. Giữ ngắn gọn, chuyên nghiệp và ấn tượng.\n");
            sb.append(
                    "9. NGÔN NGỮ: Công việc này sử dụng tiếng Việt. TẤT CẢ nội dung bạn viết (headline, summary, bulletPoints, suggestions) PHẢI là tiếng Việt HOÀN TOÀN. KHÔNG được trộn lẫn tiếng Anh và tiếng Việt.\n\n");
            sb.append("ĐỊNH DẠNG ĐẦU RA: DUY NHẤT một JSON hợp lệ (không markdown, không giải thích):\n");
            sb.append("{\n" +
                    "  \"personal\": { \"fullName\": \"...\", \"headline\": \"...\", \"summary\": \"...\", \"location\": \"...\" },\n"
                    +
                    "  \"contact\": { \"email\": \"...\", \"phone\": \"...\", \"linkedin\": \"...\", \"website\": \"...\" },\n"
                    +
                    "  \"experience\": [\n" +
                    "    {\n" +
                    "      \"id\": \"<id từ DB>\",\n" +
                    "      \"role\": \"...\",\n" +
                    "      \"company\": \"...\",\n" +
                    "      \"location\": \"...\",\n" +
                    "      \"startDate\": \"...\",\n" +
                    "      \"endDate\": \"...\",\n" +
                    "      \"bulletPoints\": [\"...\"],\n" +
                    "      \"relevant\": true/false,\n" +
                    "      \"relevanceReason\": \"<lý do tại sao liên quan hay không liên quan>\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"education\": [ { \"id\": \"<id từ DB>\", \"school\": \"...\", \"degree\": \"...\", \"startDate\": \"...\", \"endDate\": \"...\" } ],\n"
                    +
                    "  \"skills\": [ { \"id\": \"<id từ DB nếu có>\", \"name\": \"...\" } ],\n" +
                    "  \"languages\": [ { \"id\": \"<id>\", \"language\": \"...\", \"proficiency\": \"...\" } ],\n" +
                    "  \"awards\": [ { \"id\": \"<id>\", \"title\": \"...\", \"issuer\": \"...\", \"year\": \"...\" } ],\n"
                    +
                    "  \"matchedSkills\": [\"...\"],\n" +
                    "  \"missingSkills\": [\"...\"],\n" +
                    "  \"suggestions\": [\"...\"],\n" +
                    "  \"overallMatchScore\": <0-100>\n" +
                    "}");
        }

        return sb.toString();
    }

    private CvSuggestionResponse mergeAiCvSuggestion(CvSuggestionResponse aiResponse, Candidate dbCandidate) {
        if (aiResponse == null) {
            return buildFallbackCvSuggestion(dbCandidate);
        }

        // Create a map of DB experiences and educations by ID for lookup
        Map<String, CandidateExperience> dbExperienceMap = new HashMap<>();
        if (dbCandidate.getExperiences() != null) {
            dbCandidate.getExperiences().forEach(exp -> dbExperienceMap.put(exp.getId(), exp));
        }

        Map<String, CandidateEducation> dbEducationMap = new HashMap<>();
        if (dbCandidate.getEducations() != null) {
            dbCandidate.getEducations().forEach(edu -> dbEducationMap.put(edu.getId(), edu));
        }

        // Process experiences: keep only those with IDs in DB, merge with AI content
        // Separate into relevant (for main list) and all (for dropdown)
        List<CvSuggestionResponse.Experience> mergedExperiences = new ArrayList<>();
        List<CvSuggestionResponse.Experience> allExperiences = new ArrayList<>();

        if (aiResponse.getExperience() != null) {
            for (CvSuggestionResponse.Experience aiExp : aiResponse.getExperience()) {
                String expId = aiExp.getId();
                CandidateExperience dbExp = dbExperienceMap.get(expId);
                if (dbExp != null) {
                    // Keep DB structure, merge with AI-generated bulletPoints
                    CvSuggestionResponse.Experience merged = CvSuggestionResponse.Experience.builder()
                            .id(expId)
                            .role(StringUtils.hasText(aiExp.getRole()) ? aiExp.getRole() : dbExp.getJobTitle())
                            .company(dbExp.getCompany() != null ? dbExp.getCompany().getName() : null)
                            .location(aiExp.getLocation())
                            .startDate(dbExp.getStartDate())
                            .endDate(dbExp.getEndDate())
                            .bulletPoints(aiExp.getBulletPoints() != null && !aiExp.getBulletPoints().isEmpty()
                                    ? aiExp.getBulletPoints()
                                    : (StringUtils.hasText(dbExp.getDescription())
                                            ? List.of(dbExp.getDescription())
                                            : Collections.emptyList()))
                            .relevant(aiExp.getRelevant())
                            .relevanceReason(aiExp.getRelevanceReason())
                            .build();

                    allExperiences.add(merged);

                    // Only add to main experience list if relevant
                    if (Boolean.TRUE.equals(aiExp.getRelevant())) {
                        mergedExperiences.add(merged);
                    }
                } else {
                    log.warn("AI returned experience with unknown ID: {}. Skipping.", expId);
                }
            }
        }

        // Process educations: keep only those with IDs in DB, merge with AI content
        List<CvSuggestionResponse.Education> mergedEducations = new ArrayList<>();
        if (aiResponse.getEducation() != null) {
            for (CvSuggestionResponse.Education aiEdu : aiResponse.getEducation()) {
                String eduId = aiEdu.getId();
                CandidateEducation dbEdu = dbEducationMap.get(eduId);
                if (dbEdu != null) {
                    // Keep DB structure, merge with AI-generated degree/school
                    CvSuggestionResponse.Education merged = CvSuggestionResponse.Education.builder()
                            .id(eduId)
                            .school(dbEdu.getEducation() != null ? dbEdu.getEducation().getOfficialName() : null)
                            .degree(StringUtils.hasText(aiEdu.getDegree()) ? aiEdu.getDegree()
                                    : firstText(dbEdu.getDegreeTitle(), dbEdu.getMajor()))
                            .startDate(dbEdu.getStartDate())
                            .endDate(dbEdu.getEndDate())
                            .build();
                    mergedEducations.add(merged);
                } else {
                    log.warn("AI returned education with unknown ID: {}. Skipping.", eduId);
                }
            }
        }

        // Validate and constrain headline length
        CvSuggestionResponse.PersonalInfo validatedPersonal = aiResponse.getPersonal();
        if (validatedPersonal != null && StringUtils.hasText(validatedPersonal.getHeadline())) {
            String truncatedHeadline = constrainHeadlineLength(validatedPersonal.getHeadline());
            validatedPersonal = CvSuggestionResponse.PersonalInfo.builder()
                    .fullName(validatedPersonal.getFullName())
                    .headline(truncatedHeadline)
                    .summary(validatedPersonal.getSummary())
                    .location(validatedPersonal.getLocation())
                    .build();
        }

        // Return merged response with both filtered and all experiences
        return CvSuggestionResponse.builder()
                .personal(validatedPersonal)
                .contact(aiResponse.getContact())
                .experience(mergedExperiences)
                .education(mergedEducations)
                .skills(aiResponse.getSkills())
                .languages(aiResponse.getLanguages())
                .awards(aiResponse.getAwards())
                .matchedSkills(aiResponse.getMatchedSkills())
                .missingSkills(aiResponse.getMissingSkills())
                .suggestions(aiResponse.getSuggestions())
                .overallMatchScore(aiResponse.getOverallMatchScore())
                .allExperiences(allExperiences)
                .build();
    }

    private record CvSourceContext(String text, String source, int uploadedCvCount) {
        private static CvSourceContext uploaded(String text, int uploadedCvCount) {
            return new CvSourceContext(text, "uploaded_cvs", uploadedCvCount);
        }

        private static CvSourceContext profile(String text) {
            return new CvSourceContext(text, "candidate_profile", 0);
        }
    }

    private Job findOwnedJob(String jobId, String companyId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found with ID: " + jobId));

        if (!job.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Job does not belong to this company");
        }

        return job;
    }

    private void syncJobSearchDocument(Job job) {
        if (job == null || job.getId() == null) {
            return;
        }

        if (!jobSearchDocumentFactory.shouldIndex(job)) {
            jobESRepository.deleteById(job.getId());
            return;
        }

        JobES jobES = jobSearchDocumentFactory.toDocument(
                job,
                embedService.embed(jobSearchDocumentFactory.buildEmbeddingText(job)));
        jobESRepository.save(jobES);
    }

    private LocalDate parseExpiryDate(String expiryDate) {
        try {
            return LocalDate.parse(expiryDate.trim());
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Expiry date must use format yyyy-MM-dd");
        }
    }

    private void applyManagedStatus(Job job, Status nextStatus) {
        Set<Status> allowedStatuses = EnumSet.of(Status.ACTIVE, Status.INACTIVE, Status.CLOSED);
        if (!allowedStatuses.contains(nextStatus)) {
            throw new BadRequestException("Unsupported job status update: " + nextStatus);
        }

        if (nextStatus == Status.ACTIVE) {
            if (!StringUtils.hasText(job.getExpiryDate())) {
                throw new BadRequestException("Active job must have an expiry date");
            }

            LocalDate expiryDate = parseExpiryDate(job.getExpiryDate());
            if (expiryDate.isBefore(LocalDate.now())) {
                throw new BadRequestException("Expiry date must be today or later to reopen job");
            }
        }

        job.setStatus(nextStatus);
    }

    private Page<Job> filterPublicPage(Page<Job> jobs, Pageable pageable) {
        if (jobs == null || jobs.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
        List<Job> filtered = jobs.getContent().stream()
                .filter(this::isJobPubliclyAvailable)
                .toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    private boolean isDeadlinePassed(String expiryDate) {
        if (!StringUtils.hasText(expiryDate)) {
            return false;
        }
        try {
            return LocalDate.now().isAfter(LocalDate.parse(expiryDate.trim()));
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

}
