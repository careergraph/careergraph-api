package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.persistence.models.ApplicationStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, String> {

  List<ApplicationStageHistory> findByApplicationJobCompanyIdAndChangedAtBetween(
      String companyId,
      LocalDateTime from,
      LocalDateTime to);

  long countByApplicationJobCompanyIdAndToStageAndChangedAtBetween(
      String companyId,
      ApplicationStage stage,
      LocalDateTime from,
      LocalDateTime to);

  List<ApplicationStageHistory> findTop20ByApplicationJobCompanyIdAndChangedAtBetweenOrderByChangedAtDesc(
      String companyId,
      LocalDateTime from,
      LocalDateTime to);
}
