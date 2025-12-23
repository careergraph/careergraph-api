package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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

    private final Integer PAGE_SIZE_PERSONAL_JOB = 8;
    private final EmbeddingModel embeddingModel;
    private final EmbedService embedService;
    private final JobESRepository jobESRepository;
    private final QueryEnrichmentService  queryEnrichmentService;

    private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;

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
//                .expiredDate(safeParseDate(job.getExpiryDate()))
                .embedding(embedService.embed(job.getTitle()+ " " +  job.getJobCategory().getDisplayName() + " " + job.getState()))
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
        List<Job> extraJobs = jobRepository.findLatestJobsExcluding(currentDate, excludeIds.isEmpty() ? null : excludeIds);

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

        String keyword = _genKey(candidate);
        // Get current date for filtering expired jobs
        Pageable pageable = PageRequest.of(0, 6);

        SearchResponse<JobES> listSearch = jobESService.searchJobsByNavtiveAndFuzzy(keyword, pageable);

        List<String> esIds = listSearch.hits().hits().stream()
                .map(Hit::id)
                .toList();
        return jobRepository.findAllById(esIds)
                .stream()
                .sorted(Comparator.comparingInt(p -> esIds.indexOf(p.getId())))
                .toList();

    }

    private String _genKey(Candidate candidate) {
        StringBuilder sb = new StringBuilder();
        sb.append(candidate.getLocations());
        sb.append(candidate.getDesiredPosition());
        sb.append(candidate.getIndustries());
        return sb.toString();
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
        List<Status> statuses = filter.getStatuses().isEmpty() ? null : filter.getStatuses();
        List<JobCategory> jobCategories = filter.getJobCategories().isEmpty() ? null : filter.getJobCategories();
        List<EmploymentType> employmentTypes = filter.getEmploymentTypes().isEmpty() ? null : filter.getEmploymentTypes();
        List<EducationType> educationTypes = null;
        List<ExperienceLevel> experienceLevels = null;
        String city = filter.getCity();

        Page<Job> jobs = null;

        if (type == PartyType.COMPANY) {
            jobs = jobRepository.searchJobForCompany(partyId, statuses, jobCategories, employmentTypes, query, pageable);
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
    public Page<Job> searchEmbed(JobFilterRequest filter, String partyId, String query, Pageable pageable, PartyType type) {
        // Check company ID
        if (type == PartyType.COMPANY && partyId == null) {
            throw new BadRequestException("Company ID is required");
        }
        // Get params from filter
        List<Status> statuses = filter.getStatuses().isEmpty() ? null : filter.getStatuses();
        List<JobCategory> jobCategories = filter.getJobCategories().isEmpty() ? null : filter.getJobCategories();
        List<EmploymentType> employmentTypes = filter.getEmploymentTypes().isEmpty() ? null : filter.getEmploymentTypes();
        List<EducationType> educationTypes = filter.getEducationTypes().isEmpty() ? null : filter.getEducationTypes();
        List<ExperienceLevel> experienceLevels = filter.getExperienceLevels().isEmpty() ? null : filter.getExperienceLevels();;;
        String city = filter.getCity();

        Page<Job> jobs = null;

        if (type == PartyType.COMPANY) {
            jobs = jobRepository.searchJobForCompany(partyId, statuses, jobCategories, employmentTypes, query, pageable);
        } else {
            String keyword="";
            if(partyId==null && (query==null||query.isEmpty())) {
                List <Job> list = getJobsForAnonymousUser();
                return new PageImpl<>(list);
            }
            if(partyId != null) {
                Candidate candidate = candidateRepository.findById(partyId)
                        .orElse(null);
                if(candidate != null && query.isEmpty()) {
                    keyword = _genKey(candidate);
                }
            }

            if(query != null) keyword = keyword + " " + query;
//            float[] queryVector = embedService.embed(queryEnrichmentService.normalizeToEnglish(keyword));
            float[] queryVector = embedService.embed(keyword);

            SearchResponse<JobES> esResponse =
                    jobESService.knnSearch(queryVector, filter, partyId, pageable, type);
//            SearchResponse<JobES> esResponse =
//                    jobESService.knnSearch(queryVector, 10);

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

}
