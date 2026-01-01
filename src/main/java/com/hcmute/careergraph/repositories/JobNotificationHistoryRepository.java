package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.persistence.models.JobNotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobNotificationHistoryRepository
        extends JpaRepository<JobNotificationHistory, String> {

    @Query("select h.jobId from JobNotificationHistory h where h.userId = :userId")
    List<String> findSentJobIds(String userId);

    boolean existsByJobIdAndUserId(String jobId, String userId);

    boolean existsByUserIdAndJobIdAndSendType(
            String userId,
            String jobId,
            SendType sendType
    );

    List<JobNotificationHistory> findByUserIdAndSendType(
            String userId,
            SendType sendType
    );
}