package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.InterviewRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRecordingRepository extends JpaRepository<InterviewRecording, String> {

    List<InterviewRecording> findByInterviewId(String interviewId);
}
