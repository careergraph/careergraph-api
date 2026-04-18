package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.response.DashboardSummaryResponse;
import com.hcmute.careergraph.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  private final AnalyticsService analyticsService;
  private final SecurityUtils securityUtils;

  @GetMapping("/dashboard-summary")
  public RestResponse<DashboardSummaryResponse> getDashboardSummary(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Authentication authentication) {
    String companyId = securityUtils.extractCompanyId(authentication);
    if (!StringUtils.hasText(companyId)) {
      throw new BadRequestException("Company ID is required");
    }

    DashboardSummaryResponse result = analyticsService.getDashboardSummary(from, to, companyId);

    return RestResponse.<DashboardSummaryResponse>builder()
        .status(HttpStatus.OK)
        .message("Dashboard summary retrieved successfully")
        .data(result)
        .build();
  }
}
