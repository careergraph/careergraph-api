package com.hcmute.careergraph.listener;

import com.hcmute.careergraph.persistence.event.ApplicationCreatedEvent;
import com.hcmute.careergraph.services.ApplicationAiScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Chạy sàng lọc AI sau khi transaction tạo application commit (tránh race với @Async).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationAiScreeningEventListener {

    private final ApplicationAiScreeningService applicationAiScreeningService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        log.info("Application AI screening queued for applicationId={}", event.applicationId());
        try {
            applicationAiScreeningService.screenApplication(event.applicationId());
        } catch (Exception ex) {
            log.error("Application AI screening listener failed for id={}", event.applicationId(), ex);
        }
    }
}
