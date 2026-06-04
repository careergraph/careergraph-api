package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.ResumeDocumentTextExtractor;
import com.hcmute.careergraph.persistence.event.CandidateUpdatedEvent;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.services.ResumeTextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeTextExtractionServiceImpl implements ResumeTextExtractionService {

    private final FileRepository fileRepository;
    private final WebClient.Builder webClientBuilder;
    private final ApplicationEventPublisher eventPublisher;
    private final CvKeywordsExtractionService cvKeywordsExtractionService;

    @Value("${application.resume-extraction.max-download-bytes:15728640}")
    private int maxDownloadBytes;

    @Value("${application.resume-extraction.max-stored-chars:50000}")
    private int maxStoredChars;

    @Value("${application.resume-extraction.allowed-hosts:res.cloudinary.com}")
    private String allowedHostsCsv;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extractAndPersistByFileId(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return;
        }
        Optional<File> opt = fileRepository.findById(fileId);
        if (opt.isEmpty()) {
            log.warn("Resume extraction: file not found id={}", fileId);
            return;
        }
        File file = opt.get();
        String url = file.getFilePath();
        if (!StringUtils.hasText(url)) {
            persistError(file, "Không có URL file (file_path).");
            return;
        }
        if (!isAllowedHost(url)) {
            persistError(file, "Host URL không nằm trong danh sách cho phép: " + hostOf(url));
            return;
        }

        try {
            byte[] bytes = download(url);
            String text = ResumeDocumentTextExtractor.extractText(bytes, file.getMimeType(), file.getFileName());
            if (!StringUtils.hasText(text)) {
                persistError(file, "Không đọc được nội dung chữ từ file (rỗng).");
                return;
            }
            String truncated = truncateForStore(text);
            String contentHash = sha256(truncated);
            if (contentHash.equals(file.getResumeContentHash())
                    && StringUtils.hasText(file.getResumeExtractedText())) {
                log.debug("Resume extraction skip: content unchanged fileId={}", fileId);
                return;
            }
            file.setResumeExtractedText(truncated);
            file.setResumeExtractionError(null);
            file.setResumeContentHash(contentHash);
            fileRepository.save(file);
            log.info("Resume extraction OK fileId={} chars={}", fileId, truncated.length());

            // V2: Trigger CV keywords extraction (async, non-blocking)
            try {
                cvKeywordsExtractionService.extractAndPersistKeywords(file.getId(), truncated);
            } catch (Exception kwEx) {
                log.warn("CV keywords extraction failed fileId={}: {}", fileId, kwEx.getMessage());
            }

            publishCandidateEvent(file, CandidateUpdatedEvent.CandidateUpdateType.RESUME_UPDATED);
        } catch (Exception ex) {
            log.warn("Resume extraction failed fileId={}: {}", fileId, ex.getMessage());
            persistError(file, shorten(ex.getMessage(), 500));
            publishCandidateEvent(file, CandidateUpdatedEvent.CandidateUpdateType.RESUME_EXTRACTION_FAILED);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extractAndPersistForCandidateResumeUrl(String candidateId, String resumeUrl) {
        if (!StringUtils.hasText(candidateId) || !StringUtils.hasText(resumeUrl)) {
            return;
        }
        String normalized = normalizeUrl(resumeUrl.trim());
        Optional<File> fileOpt = fileRepository
                .findFirstByOwnerIdAndFilePathAndStatusOrderByCreatedDateDesc(candidateId, resumeUrl.trim(),
                        Status.ACTIVE);
        Optional<File> match = fileOpt;
        if (match.isEmpty() && !normalized.equals(resumeUrl.trim())) {
            match = fileRepository.findFirstByOwnerIdAndFilePathAndStatusOrderByCreatedDateDesc(candidateId,
                    normalized, Status.ACTIVE);
        }
        if (match.isEmpty()) {
            log.info("Resume extraction: no file row for candidate={} url (normalized lookup skipped)", candidateId);
            return;
        }
        extractAndPersistByFileId(match.get().getId());
    }

    private void persistError(File file, String message) {
        file.setResumeExtractedText(null);
        file.setResumeExtractionError(message);
        file.setResumeContentHash(null);
        fileRepository.save(file);
    }

    private void publishCandidateEvent(File file, CandidateUpdatedEvent.CandidateUpdateType updateType) {
        if (file != null && StringUtils.hasText(file.getOwnerId())) {
            eventPublisher.publishEvent(new CandidateUpdatedEvent(file.getOwnerId(), updateType));
        }
    }

    private byte[] download(String url) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxDownloadBytes))
            .build();
        WebClient client = webClientBuilder
            .exchangeStrategies(strategies)
            .build();
        byte[] body = client.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(90))
                .block();
        if (body == null) {
            throw new IllegalStateException("Tải file trả về rỗng.");
        }
        if (body.length > maxDownloadBytes) {
            throw new IllegalStateException("File vượt quá giới hạn tải (" + maxDownloadBytes + " bytes).");
        }
        return body;
    }

    private boolean isAllowedHost(String url) {
        String host = hostOf(url);
        if (!StringUtils.hasText(host)) {
            return false;
        }
        List<String> allowed = Arrays.stream(allowedHostsCsv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .toList();
        String h = host.toLowerCase(Locale.ROOT);
        return allowed.stream().anyMatch(a -> h.equals(a) || h.endsWith("." + a));
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeUrl(String url) {
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private String truncateForStore(String text) {
        if (text.length() <= maxStoredChars) {
            return text;
        }
        return text.substring(0, maxStoredChars) + "\n...[truncated]";
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.replace("\n", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot calculate resume content hash", e);
        }
    }
}
