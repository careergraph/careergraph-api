package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.config.app.CandidateElasticsearchDataInitializer;
import com.hcmute.careergraph.config.app.ElasticsearchDataInitializer;
import com.hcmute.careergraph.config.app.ElasticsearchSyncResult;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.services.impl.ExpiredJobRepairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("internal/elasticsearch")
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class InternalElasticsearchSyncController {

  private final ElasticsearchDataInitializer jobSyncInitializer;
  private final CandidateElasticsearchDataInitializer candidateSyncInitializer;
  private final ExpiredJobRepairService expiredJobRepairService;

  @Value("${socket.internal.api-key:dev-secret-change-in-prod}")
  private String internalApiKey;

  @PostMapping("/sync")
  public RestResponse<Map<String, ElasticsearchSyncResult>> sync(
      @RequestHeader("x-internal-api-key") String providedInternalApiKey,
      @RequestParam(defaultValue = "all") String target,
      @RequestParam(required = false) Integer jobBatchSize,
      @RequestParam(required = false) Integer candidateBatchSize,
      @RequestParam(defaultValue = "false") boolean force,
      @RequestParam(required = false) Boolean forceJobs,
      @RequestParam(required = false) Boolean forceCandidates) {
    validateInternalApiKey(providedInternalApiKey);

    String normalizedTarget = target.trim().toLowerCase(Locale.ROOT);
    log.info(
      "Manual Elasticsearch sync requested: target={}, force={}, forceJobs={}, forceCandidates={}, jobBatchSize={}, candidateBatchSize={}",
      normalizedTarget,
      force,
      forceJobs,
      forceCandidates,
      jobBatchSize,
      candidateBatchSize);
    Map<String, ElasticsearchSyncResult> results = new LinkedHashMap<>();

    if (normalizedTarget.equals("all")
        || normalizedTarget.equals("jobs")
        || normalizedTarget.equals("expired-jobs")) {
      results.put("expired-jobs", expiredJobRepairService.repairExpiredJobs());
    }

    if (normalizedTarget.equals("all") || normalizedTarget.equals("jobs")) {
      boolean effectiveForceJobs = forceJobs != null ? forceJobs : force;
      results.put("jobs", jobSyncInitializer.syncNow(effectiveForceJobs, jobBatchSize));
    }

    if (normalizedTarget.equals("all") || normalizedTarget.equals("candidates")) {
      boolean effectiveForceCandidates = forceCandidates != null ? forceCandidates : force;
      results.put("candidates", candidateSyncInitializer.syncNow(effectiveForceCandidates, candidateBatchSize));
    }

    if (results.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid target. Supported values: all, jobs, candidates, expired-jobs");
    }

    log.info("Manual Elasticsearch sync finished: targets={}", results.keySet());

    return RestResponse.<Map<String, ElasticsearchSyncResult>>builder()
        .status(HttpStatus.OK)
        .message("Elasticsearch sync request completed")
        .data(results)
        .build();
  }

  private void validateInternalApiKey(String providedInternalApiKey) {
    if (!StringUtils.hasText(providedInternalApiKey) || !providedInternalApiKey.equals(internalApiKey)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
    }
  }
}
