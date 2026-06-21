package com.hcmute.careergraph.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.CandidateSearchTextBuilder;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.FastAPIClientService;
import com.hcmute.careergraph.services.JobESService;
import com.hcmute.careergraph.services.RedisService;
import com.hcmute.careergraph.mapper.JobMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private JobESService jobESService;
    @Mock
    private EmbedService embedService;
    @Mock
    private JobESRepository jobESRepository;
    @Mock
    private FastAPIClientService fastAPIClientService;
    @Mock
    private CandidateSearchTextBuilder candidateSearchTextBuilder;
    @Mock
    private RedisService redisService;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private CompanyAccessPolicyService companyAccessPolicyService;
    @Mock
    private ApplicationEventPublisher publisher;

    private JobServiceImpl jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(
                jobRepository,
                companyRepository,
                candidateRepository,
                jobMapper,
                jobESService,
                new ObjectMapper(),
                embedService,
                jobESRepository,
                fastAPIClientService,
                candidateSearchTextBuilder,
                redisService,
                fileRepository,
                companyAccessPolicyService,
                publisher
        );
        ReflectionTestUtils.setField(jobService, "maxUploadedCvContextChars", 24_000);
    }

    @Test
    void generateCv_shouldUseAllActiveUploadedCvTextsAsPromptContext() {
        Job job = createJob();
        Candidate candidate = createCandidate();

        File latestResume = createResumeFile(
                "resume-latest.pdf",
                "Java Spring Boot microservices experience",
                "hash-1",
                LocalDateTime.of(2026, 6, 16, 10, 0)
        );
        File olderResume = createResumeFile(
                "resume-older.pdf",
                "React TypeScript frontend architecture",
                "hash-2",
                LocalDateTime.of(2026, 6, 15, 10, 0)
        );

        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(redisService.getObject("cv_suggestion_limit:candidate-1", Integer.class)).thenReturn(null);
        when(fileRepository.findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                eq("candidate-1"),
                eq(Status.ACTIVE),
                anyList()))
                .thenReturn(List.of(latestResume, olderResume));
        when(fastAPIClientService.cvSuggestion(any())).thenReturn("""
                {
                  "personal": { "fullName": "Nguyen Van A", "headline": "Backend Developer", "summary": "Summary", "location": "HCM" },
                  "contact": { "email": "a@example.com", "phone": "0123" },
                  "experience": [],
                  "education": [],
                  "skills": [],
                  "matchedSkills": ["Java"],
                  "missingSkills": ["AWS"],
                  "suggestions": ["Add cloud delivery achievements"],
                  "overallMatchScore": 85
                }
                """);

        var result = jobService.generateCv("job-1", "candidate-1");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fastAPIClientService).cvSuggestion(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("Java Spring Boot microservices experience");
        assertThat(prompt).contains("React TypeScript frontend architecture");
        assertThat(prompt).contains("[UPLOADED_CV_1 | resume-latest.pdf");
        assertThat(prompt).contains("[UPLOADED_CV_2 | resume-older.pdf");
        assertThat(result.getOverallMatchScore()).isEqualTo(85);
        verify(redisService).setObject("cv_suggestion_limit:candidate-1", 1, 86400);
    }

    @Test
    void generateCv_shouldFallbackToCandidateProfileWhenUploadedCvsHaveNoExtractedText() {
        Job job = createJob();
        Candidate candidate = createCandidate();

        File blankResume = createResumeFile(
                "resume-empty.pdf",
                "   ",
                "hash-empty",
                LocalDateTime.of(2026, 6, 16, 11, 0)
        );

        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(redisService.getObject("cv_suggestion_limit:candidate-1", Integer.class)).thenReturn(null);
        when(fileRepository.findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                eq("candidate-1"),
                eq(Status.ACTIVE),
                anyList()))
                .thenReturn(List.of(blankResume));
        when(fastAPIClientService.cvSuggestion(any())).thenReturn("""
                {
                  "personal": { "fullName": "Nguyen Van A", "headline": "Backend Developer", "summary": "Summary", "location": "HCM" },
                  "contact": { "email": "a@example.com", "phone": "0123" },
                  "experience": [],
                  "education": [],
                  "skills": [],
                  "matchedSkills": [],
                  "missingSkills": [],
                  "suggestions": [],
                  "overallMatchScore": 70
                }
                """);

        jobService.generateCv("job-1", "candidate-1");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fastAPIClientService).cvSuggestion(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("Senior Backend Developer");
        assertThat(prompt).contains("Build scalable hiring products");
        assertThat(prompt).doesNotContain("[UPLOADED_CV_1 | resume-empty.pdf");
    }

    @Test
    void createJob_shouldRejectUnverifiedCompany() {
        Company company = new Company();
        company.setId("company-1");
        var request = com.hcmute.careergraph.persistence.dtos.request.JobCreationRequest.builder()
                .title("Backend Engineer")
                .description("Build systems")
                .state("HCM")
                .city("Thu Duc")
                .build();
        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));
        doThrow(new com.hcmute.careergraph.exception.BadRequestException("blocked"))
                .when(companyAccessPolicyService)
                .assertCompanyCanManageJobs(company);

        assertThatThrownBy(() -> jobService.createJob(request, "company-1"))
                .isInstanceOf(com.hcmute.careergraph.exception.BadRequestException.class)
                .hasMessage("blocked");
    }

    private Job createJob() {
        Company company = new Company();
        company.setName("CareerGraph");

        Job job = new Job();
        job.setId("job-1");
        job.setTitle("Backend Developer");
        job.setDescription("Build resilient APIs for hiring workflows.");
        job.setQualifications(List.of("Java", "Spring Boot", "AWS"));
        job.setCompany(company);
        return job;
    }

    private Candidate createCandidate() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setFirstName("Nguyen");
        candidate.setLastName("Van A");
        candidate.setCurrentJobTitle("Senior Backend Developer");
        candidate.setDesiredPosition("Technical Lead");
        candidate.setSummary("Build scalable hiring products");
        candidate.setIndustry("Software");
        candidate.setIndustries(new ArrayList<>(List.of("Software", "HR Tech")));
        candidate.setLocations(new ArrayList<>(List.of("Ho Chi Minh City")));
        candidate.setWorkTypes(new ArrayList<>(List.of("Hybrid")));
        candidate.setSkills(new HashSet<>());
        candidate.setExperiences(new HashSet<>());
        candidate.setEducations(new HashSet<>());
        candidate.setContacts(new HashSet<>());
        candidate.setAddresses(new HashSet<>());
        return candidate;
    }

    private File createResumeFile(String fileName, String text, String hash, LocalDateTime createdDate) {
        File file = new File();
        file.setFileName(fileName);
        file.setResumeExtractedText(text);
        file.setResumeContentHash(hash);
        file.setCreatedDate(createdDate);
        file.setFileType(FileType.RESUME);
        file.setStatus(Status.ACTIVE);
        return file;
    }
}
