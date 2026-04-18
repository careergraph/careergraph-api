package com.hcmute.careergraph.listener;

import com.hcmute.careergraph.persistence.event.ResumeFilePersistedEvent;
import com.hcmute.careergraph.services.ResumeTextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Trích text CV nền ngay sau khi upload (không chặn API upload).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeTextExtractionEventListener {

    private final ResumeTextExtractionService resumeTextExtractionService;

    @EventListener
    @Async
    public void onResumeFilePersisted(ResumeFilePersistedEvent event) {
        try {
            resumeTextExtractionService.extractAndPersistByFileId(event.fileId());
        } catch (Exception ex) {
            log.error("Resume extraction listener failed fileId={}", event.fileId(), ex);
        }
    }
}
