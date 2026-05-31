package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.EducationType;
import com.hcmute.careergraph.enums.job.EmploymentType;
import com.hcmute.careergraph.enums.job.ExperienceLevel;
import com.hcmute.careergraph.enums.job.JobCategory;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.mapper.JobMapper;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import com.hcmute.careergraph.persistence.dtos.request.JobRecruimentRequest;
import com.hcmute.careergraph.persistence.dtos.response.CvSuggestionResponse;
import com.hcmute.careergraph.persistence.event.JobCreatedEvent;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final ApplicationEventPublisher publisher;

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

        // 3. Map request -> entity
        Job job = jobMapper.toEntity(request, company);
        JobES jobES = JobES.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .status(job.getStatus().name())
                .jobCategory(job.getJobCategory().name())
                .employmentType(job.getEmploymentType().name())
                .experienceLevel(job.getExperienceLevel().name())
                .education(job.getEducation().name())
                .state(job.getState())
                .city(job.getCity())
                .companyId(job.getCompany().getId())
                .qualifications(job.getQualifications())
                .minimumQualifications(job.getMinimumQualifications())
                .responsibilities(job.getResponsibilities())
                // .expiredDate(safeParseDate(job.getExpiryDate()))
                .embedding(embedService.embed(buildJobSearchText(job)))
                .build();
        jobESRepository.save(jobES);
        // 4. Lưu vào database
        Job savedJob = jobRepository.save(job);
        log.info("Job created successfully with ID: {}", savedJob.getId());
        publisher.publishEvent(new JobCreatedEvent(savedJob.getId()));
        return savedJob;
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

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        // Increase views of job
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
    public Page<Job> getAllJobs(Pageable pageable) {
        log.info("Fetching all jobs");

        Page<Job> jobs = jobRepository.findAll(pageable);
        return jobs;
    }

    /**
     * Update job
     */
    @Transactional
    @Override
    public Job updateJob(String jobId, JobCreationRequest request, String companyId) {
        // TODO: Implement update logic
        throw new UnsupportedOperationException("Update job not implemented yet");
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

        // Update job
        job.get().setStatus(Status.ACTIVE);
        jobRepository.save(job.get());

        return job.get();
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
        jobRepository.save(job);

        log.info("Job deleted successfully with ID: {}", jobId);
    }

    @Transactional
    @Override
    public void activateJob(String jobId, String companyId) {

    }

    @Transactional
    @Override
    public void deactivateJob(String jobId, String companyId) {

    }

    @Transactional(readOnly = true)
    @Override
    public Page<Job> getJobByCategory(JobCategory jobCategory, Pageable pageable) {

        if (jobCategory == null) {
            return Page.empty();
        }

        return jobRepository.findByJobCategory(jobCategory, pageable);
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
        List<Job> personalJobs = jobRepository.findJobByPersonalized(userId, currentDate);
        if (personalJobs.size() >= PAGE_SIZE_PERSONAL_JOB) {
            return personalJobs.subList(0, PAGE_SIZE_PERSONAL_JOB);
        }

        int remaining = PAGE_SIZE_PERSONAL_JOB - personalJobs.size();
        List<String> excludeIds = personalJobs.stream().map(Job::getId).toList();
        List<Job> extraJobs = jobRepository.findLatestJobsExcluding(currentDate,
                excludeIds.isEmpty() ? null : excludeIds);

        List<Job> result = new ArrayList<>(personalJobs);
        result.addAll(extraJobs);

        return result;
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

        String keyword = candidateSearchTextBuilder.build(candidate, true);
        if (keyword == null || keyword.trim().length() <= 0) {
            return getJobsForAnonymousUser();
        }
        log.debug("Personalized job search text built for candidateId={}, chars={}", userId, keyword.length());
        // Get current date for filtering expired jobs
        Pageable pageable = PageRequest.of(0, 6);

        SearchResponse<JobES> listSearch = jobESService.searchJobsByNavtiveAndFuzzy(keyword, pageable);
        if (listSearch == null || listSearch.hits() == null || listSearch.hits().hits().isEmpty()) {
            log.warn("Personalized ES search returned no usable response for candidateId={}, falling back to latest jobs",
                    userId);
            return getJobsForAnonymousUser();
        }

        List<String> esIds = listSearch.hits().hits().stream()
                .map(Hit::id)
                .toList();
        return jobRepository.findAllById(esIds)
                .stream()
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

        // Get new job
        List<Job> newJobs = jobRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(0, 8));
        if (newJobs == null) {
            return new ArrayList<>();
        }

        return newJobs;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Job> getJobsPopular() {

        List<Job> jobsPopular = jobRepository.findPopularJob();
        if (jobsPopular.isEmpty()) {
            return new ArrayList<>();
        }

        return jobsPopular;
    }

    @Override
    public Page<Job> getSimilarJob(String jobId, Pageable pageable) {

        Page<Job> jobsSimilar = jobRepository.findSimilarJob(jobId, pageable);
        if (jobsSimilar == null) {
            return new PageImpl<>(null);
        }

        return jobsSimilar;
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
        String city = filter.getCity();

        Page<Job> jobs = null;

        if (type == PartyType.COMPANY) {
            jobs = jobRepository.searchJobForCompany(partyId, statuses, jobCategories, employmentTypes, query,
                    pageable);
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

            jobs = jobRepository.searchJobForCandidate(partyId, city, jobCategories, employmentTypes,
                    experienceLevels, educationTypes, query, pageable);
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
        String city = filter.getCity();

        Page<Job> jobs = null;

        if (type == PartyType.COMPANY) {
            jobs = jobRepository.searchJobForCompany(partyId, statuses, jobCategories, employmentTypes, query,
                    pageable);
        } else {
            String keyword = "";

            // Nếu không có keyword → dùng filter-only search (match_all + post filter)
            boolean hasKeyword = query != null && !query.trim().isEmpty();

            if (partyId != null) {
                Candidate candidate = candidateRepository.findById(partyId)
                        .orElse(null);
                if (candidate != null && !hasKeyword) {
                    keyword = candidateSearchTextBuilder.build(candidate, true);
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
                    .sorted(Comparator.comparingInt(j -> ids.indexOf(j.getId())))
                    .toList();

            assert esResponse.hits().total() != null;
            long total = esResponse.hits().total().value();

            return new PageImpl<>(ljobs, pageable, total);
        }
        return jobs;
    }

    @Override
    public CvSuggestionResponse generateCv(String jobId, String candidateId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        // 2. Xây dựng Prompt (Câu lệnh cho AI)
        String prompt = buildCvGenerationPrompt(job, candidate);

        // 3. Gọi AI
        String jsonResponse = fastAPIClientService.cvSuggestion(prompt);

        // 4. Parse kết quả JSON từ AI thành Object
        try {
            // Đôi khi AI trả về markdown ```json ... ```, cần clean trước khi parse
            String cleanJson = cleanJsonString(jsonResponse);
            return objectMapper.readValue(cleanJson, CvSuggestionResponse.class);
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            throw new RuntimeException("Failed to generate CV suggestion", e);
        }
    }

    // Helper để làm sạch chuỗi JSON nếu AI trả về dạng Markdown
    private String cleanJsonString(String response) {
        if (response.contains("```json")) {
            return response.replace("```json", "").replace("```", "").trim();
        }
        if (response.contains("```")) {
            return response.replace("```", "").trim();
        }
        return response;
    }

    private String buildCvGenerationPrompt(Job job, Candidate candidate) {
        StringBuilder sb = new StringBuilder();

        sb.append("Đóng vai là một chuyên gia tư vấn nghề nghiệp và viết CV chuyên nghiệp (Top CV Writer). ");
        sb.append(
                "Nhiệm vụ của bạn là viết lại nội dung CV cho ứng viên để phù hợp nhất với công việc đang ứng tuyển.\n\n");

        // --- Input: Job Info ---
        sb.append("--- THÔNG TIN CÔNG VIỆC (TARGET JOB) ---\n");
        sb.append("Vị trí: ").append(job.getTitle()).append("\n");
        sb.append("Công ty: ").append(job.getCompany().getName()).append("\n");
        sb.append("Mô tả công việc: ").append(job.getDescription()).append("\n");
        sb.append("Yêu cầu kỹ năng: ").append(job.getQualifications()).append("\n\n");

        // --- Input: Candidate Info ---
        sb.append("--- HỒ SƠ GỐC CỦA ỨNG VIÊN ---\n");
        sb.append("Họ tên: ").append(candidate.getFirstName() + " " + candidate.getLastName()).append("\n");
        List<String> skills = candidate.getSkills().stream()
                .map(skill -> skill.getSkill().getName())
                .toList();
        sb.append("Kỹ năng hiện có: ").append(skills).append("\n");

        List<String> experiences = candidate.getExperiences().stream()
                .map(experience -> experience.getCompany().getName() + ": from " + experience.getStartDate()
                        + " to " + experience.getEndDate())
                .toList();
        sb.append("Kinh nghiệm làm việc: ").append(experiences).append("\n");

        List<String> educations = candidate.getEducations().stream()
                .map(experience -> experience.getEducation().getOfficialName() + ": from " + experience.getStartDate()
                        + " to " + experience.getEndDate())
                .toList();
        sb.append("Học vấn: ").append(educations).append("\n\n");

        // --- Output Requirement ---
        sb.append("--- YÊU CẦU ĐẦU RA ---\n");
        sb.append(
                "1. Hãy viết lại phần 'summary' (tóm tắt) thật ấn tượng, thể hiện ứng viên là người phù hợp cho vị trí này.\n");
        sb.append(
                "2. Trong phần 'experience', hãy viết lại các 'bulletPoints' sao cho làm nổi bật các từ khóa (keywords) có trong mô tả công việc (Job Description).\n");
        sb.append("3. Chỉ giữ lại hoặc sắp xếp các kỹ năng (skills) liên quan lên đầu.\n");
        sb.append(
                "4. Trả về kết quả DUY NHẤT là một chuỗi JSON hợp lệ khớp với cấu trúc sau (không giải thích thêm):\n");

        // Cung cấp mẫu JSON để AI điền vào
        sb.append("{\n" +
                "  \"personal\": { \"fullName\": \"...\", \"headline\": \"...\", \"summary\": \"...\", \"location\": \"...\" },\n"
                +
                "  \"contact\": { \"email\": \"...\", \"phone\": \"...\", \"linkedin\": \"...\" },\n" +
                "  \"experience\": [ { \"role\": \"...\", \"company\": \"...\", \"startDate\": \"...\", \"endDate\": \"...\", \"bulletPoints\": [\"...\"] } ],\n"
                +
                "  \"education\": [ { \"school\": \"...\", \"degree\": \"...\", \"startDate\": \"...\", \"endDate\": \"...\" } ],\n"
                +
                "  \"skills\": [ { \"name\": \"...\" } ]\n" +
                "}");

        return sb.toString();
    }

}
