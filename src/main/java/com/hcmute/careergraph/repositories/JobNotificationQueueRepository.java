package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.enums.job.StatusSend;
import com.hcmute.careergraph.persistence.models.JobNotificationHistory;
import com.hcmute.careergraph.persistence.models.JobNotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobNotificationQueueRepository
        extends JpaRepository<JobNotificationQueue, String> {

    List<JobNotificationQueue> findBySendTypeAndStatusSend(
            SendType sendType,
            StatusSend status
    );
    boolean existsByUserIdAndJobIdAndSendType(
            String userId,
            String jobId,
            SendType sendType
    );
}
