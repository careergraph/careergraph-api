package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.persistence.dtos.request.CompanyStageRequests;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyRecruitmentStage;

import java.util.List;

public interface CompanyRecruitmentStageService {

    List<CompanyRecruitmentStage> getCompanyStages(String companyId);

    List<CompanyRecruitmentStage> updateCompanyStages(String companyId,
                                                      List<CompanyStageRequests.StageConfig> stages);

    void initializeDefaultStages(Company company);

    boolean isStageActiveForCompany(String companyId, ApplicationStage stage);
}
