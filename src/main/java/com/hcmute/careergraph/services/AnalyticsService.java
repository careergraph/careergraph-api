package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.DashboardSummaryResponse;

import java.time.LocalDate;

public interface AnalyticsService {

  DashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to, String companyId);
}
