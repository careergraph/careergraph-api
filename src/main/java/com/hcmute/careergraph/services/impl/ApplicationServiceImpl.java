package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.mapper.ApplicationMapper;
import com.hcmute.careergraph.persistence.dtos.response.ApplicationDto;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    public ApplicationDto createApplication(ApplicationRequest request) {
        log.info("Creating new application for candidate: {} to job: {}", 
                request.getCandidateId(), request.getJobId());
        
        // Find candidate and job
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + request.getCandidateId()));
        
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + request.getJobId()));

        // Create application entity
        Application application = Application.builder()
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .rating(request.getRating())
                .notes(request.getNotes())
                .appliedDate(request.getAppliedDate() != null ? request.getAppliedDate() : 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status(Status.ACTIVE)
                .candidate(candidate)
                .job(job)
                .build();

        Application savedApplication = applicationRepository.save(application);
        log.info("Application created successfully with id: {}", savedApplication.getId());
        
        return applicationMapper.toDto(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDto getApplicationById(String id) {
        log.info("Getting application by id: {}", id);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        return applicationMapper.toDto(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getAllApplications(Pageable pageable) {
        log.info("Getting all applications with pagination");
        Page<Application> applications = applicationRepository.findAll(pageable);
        return applications.map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getApplicationsByCandidate(String candidateId, Pageable pageable) {
        log.info("Getting applications by candidate id: {}", candidateId);
        Page<Application> applications = applicationRepository.findByCandidateId(candidateId, pageable);
        return applications.map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getApplicationsByJob(String jobId, Pageable pageable) {
        log.info("Getting applications by job id: {}", jobId);
        Page<Application> applications = applicationRepository.findByJobId(jobId, pageable);
        return applications.map(applicationMapper::toDto);
    }

    @Override
    public ApplicationDto updateApplication(String id, ApplicationRequest request) {
        log.info("Updating application with id: {}", id);
        
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));

        // Update fields
        application.setCoverLetter(request.getCoverLetter());
        application.setResumeUrl(request.getResumeUrl());
        application.setRating(request.getRating());
        application.setNotes(request.getNotes());

        // Update candidate if changed
        if (!application.getCandidate().getId().equals(request.getCandidateId())) {
            Candidate candidate = candidateRepository.findById(request.getCandidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + request.getCandidateId()));
            application.setCandidate(candidate);
        }

        // Update job if changed
        if (!application.getJob().getId().equals(request.getJobId())) {
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + request.getJobId()));
            application.setJob(job);
        }

        Application updatedApplication = applicationRepository.save(application);
        log.info("Application updated successfully with id: {}", updatedApplication.getId());
        
        return applicationMapper.toDto(updatedApplication);
    }

    @Override
    public void deleteApplication(String id) {
        log.info("Deleting application with id: {}", id);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        application.softDelete();
        applicationRepository.save(application);
        log.info("Application soft deleted successfully with id: {}", id);
    }

    @Override
    public void updateApplicationStatus(String id, String status) {
        log.info("Updating application status with id: {} to status: {}", id, status);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        
        try {
            Status newStatus = Status.valueOf(status.toUpperCase());
            application.setStatus(newStatus);
            applicationRepository.save(application);
            log.info("Application status updated successfully with id: {}", id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
    }
}
