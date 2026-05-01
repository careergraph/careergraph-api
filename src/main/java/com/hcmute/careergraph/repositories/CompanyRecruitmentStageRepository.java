package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.CompanyRecruitmentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRecruitmentStageRepository extends JpaRepository<CompanyRecruitmentStage, String> {

    List<CompanyRecruitmentStage> findByCompanyIdOrderByDisplayOrderAsc(String companyId);

    List<CompanyRecruitmentStage> findByCompanyIdAndStatusOrderByDisplayOrderAsc(String companyId, Status status);

    Optional<CompanyRecruitmentStage> findByCompanyIdAndStage(String companyId, ApplicationStage stage);
}
