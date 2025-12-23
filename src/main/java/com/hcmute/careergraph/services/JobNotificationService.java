package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.event.JobCreatedEvent;

public interface JobNotificationService {
    void onJobCreated(JobCreatedEvent event);
}
