package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.SavedJob;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.repositories.SavedJobRepository;
import com.hcmute.careergraph.services.SavedJobService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SavedJobServiceImpl implements SavedJobService {

    CandidateRepository candidateRepository;
    JobRepository jobRepository;
    SavedJobRepository savedJobRepository;

    @Override
    public void saveJob(String candidateId, String jobId) {
        Candidate candidate = candidateRepository.findById(candidateId).orElseThrow(
                () -> new AppException(ErrorType.NOT_FOUND,"Candidate id not found!")
        );
        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new AppException(ErrorType.NOT_FOUND,"Job id not found!")
        );

        // Kiểm tra tồn tại
        if (savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new RuntimeException("Đã lưu rồi");
        }

        SavedJob saved = new SavedJob();
        saved.setCandidate(candidate);
        saved.setJob(job);
        savedJobRepository.save(saved);
    }

    @Override
    public void unsaveJob(String candidateId, String jobId) {
        SavedJob saved = savedJobRepository.findByCandidateIdAndJobId(candidateId, jobId);
        if(saved == null) {
            throw new AppException(ErrorType.NOT_FOUND, "Công việc này chưa lưu nên không thể hủy");
        }
        savedJobRepository.delete(saved);
    }

    @Override
    public boolean existsByCandidateIdAndJobId(String candidateId, String jobId) {
        return savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId);
    }
}
