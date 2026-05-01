package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.dtos.request.CompanyStageRequests;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyRecruitmentStage;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.CompanyRecruitmentStageRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyRecruitmentStageServiceImpl implements CompanyRecruitmentStageService {

    private final CompanyRepository companyRepository;
    private final CompanyRecruitmentStageRepository companyRecruitmentStageRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public List<CompanyRecruitmentStage> getCompanyStages(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        List<CompanyRecruitmentStage> stages = companyRecruitmentStageRepository
                .findByCompanyIdOrderByDisplayOrderAsc(company.getId());

        if (stages.isEmpty()) {
            initializeDefaultStages(company);
            stages = companyRecruitmentStageRepository
                    .findByCompanyIdOrderByDisplayOrderAsc(company.getId());
        }

        if (stages.isEmpty()) {
            return buildFallbackStages(company);
        }

        return stages;
    }

    @Override
    @Transactional
    public List<CompanyRecruitmentStage> updateCompanyStages(String companyId,
            List<CompanyStageRequests.StageConfig> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new BadRequestException("Stage configuration is required");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        Set<ApplicationStage> configurable = EnumSet.copyOf(ApplicationStage.getConfigurableStages());
        Set<ApplicationStage> requestedStages = EnumSet.noneOf(ApplicationStage.class);
        for (CompanyStageRequests.StageConfig config : stages) {
            requestedStages.add(config.stage());
        }

        if (!requestedStages.containsAll(configurable) || requestedStages.size() != configurable.size()) {
            throw new BadRequestException("Stage configuration must include all supported stages");
        }

        List<CompanyRecruitmentStage> existing = companyRecruitmentStageRepository
                .findByCompanyIdOrderByDisplayOrderAsc(companyId);
        if (existing.isEmpty()) {
            initializeDefaultStages(company);
            existing = companyRecruitmentStageRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId);
        }

        Map<ApplicationStage, CompanyRecruitmentStage> existingMap = new HashMap<>();
        for (CompanyRecruitmentStage stage : existing) {
            existingMap.put(stage.getStage(), stage);
        }

        Map<ApplicationStage, CompanyStageRequests.StageConfig> configMap = new HashMap<>();
        for (CompanyStageRequests.StageConfig config : stages) {
            if (!ApplicationStage.isConfigurableStage(config.stage())) {
                throw new BadRequestException("Stage " + config.stage() + " is not supported");
            }
            configMap.put(config.stage(), config);
        }

        boolean hasExplicitOrder = stages.stream().allMatch(config -> config.displayOrder() != null);
        List<CompanyStageRequests.StageConfig> orderedConfigs = new ArrayList<>(stages);
        if (hasExplicitOrder) {
            orderedConfigs.sort((a, b) -> Integer.compare(a.displayOrder(), b.displayOrder()));
        }

        List<CompanyRecruitmentStage> updated = new ArrayList<>();
        int order = 1;
        for (CompanyStageRequests.StageConfig config : orderedConfigs) {
            ApplicationStage stage = config.stage();
            CompanyRecruitmentStage setting = existingMap.get(stage);
            if (setting == null) {
                setting = CompanyRecruitmentStage.builder()
                        .company(company)
                        .stage(stage)
                        .build();
            }

                boolean nextActive = config.active() != null
                    ? Boolean.TRUE.equals(config.active())
                    : setting.getStatus() == null || setting.isActive();

            if (!nextActive && ApplicationStage.isRequiredStage(stage)) {
                throw new BadRequestException("Required stage cannot be disabled: " + stage);
            }

            if (!nextActive && setting.isActive()) {
                long activeCount = applicationRepository.countByJobCompanyIdAndCurrentStageIn(
                        companyId,
                        List.of(stage));
                if (activeCount > 0) {
                    throw new BadRequestException("Không thể tắt trạng thái đang có ứng viên");
                }
            }

            setting.setDisplayOrder(order++);
            if (nextActive) {
                setting.activate();
            } else {
                setting.deactivate();
            }

            updated.add(setting);
        }

        List<CompanyRecruitmentStage> saved = companyRecruitmentStageRepository.saveAll(updated);
        syncCompanyFlags(company, saved);
        return saved;
    }

    @Override
    @Transactional
    public void initializeDefaultStages(Company company) {
        if (company == null || company.getId() == null) {
            return;
        }

        List<CompanyRecruitmentStage> existing = companyRecruitmentStageRepository
                .findByCompanyIdOrderByDisplayOrderAsc(company.getId());
        if (!existing.isEmpty()) {
            return;
        }

        boolean offerBeforeTrial = company.getOfferBeforeTrial() == null
                || Boolean.TRUE.equals(company.getOfferBeforeTrial());
        boolean enableOffboardedStage = Boolean.TRUE.equals(company.getEnableOffboardedStage());

        List<ApplicationStage> defaultOrder = ApplicationStage.getDefaultPipelineOrder(offerBeforeTrial);
        List<CompanyRecruitmentStage> stages = new ArrayList<>();
        int order = 1;
        for (ApplicationStage stage : defaultOrder) {
            CompanyRecruitmentStage setting = CompanyRecruitmentStage.builder()
                    .company(company)
                    .stage(stage)
                    .displayOrder(order++)
                    .build();

            if (stage == ApplicationStage.OFFBOARDED && !enableOffboardedStage) {
                setting.deactivate();
            }

            stages.add(setting);
        }

        List<CompanyRecruitmentStage> saved = companyRecruitmentStageRepository.saveAll(stages);
        syncCompanyFlags(company, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStageActiveForCompany(String companyId, ApplicationStage stage) {
        if (!ApplicationStage.isConfigurableStage(stage)) {
            return true;
        }

        List<CompanyRecruitmentStage> stages = getCompanyStages(companyId);
        for (CompanyRecruitmentStage setting : stages) {
            if (setting.getStage() == stage) {
                return setting.isActive();
            }
        }

        return true;
    }

    private void syncCompanyFlags(Company company, List<CompanyRecruitmentStage> stages) {
        if (company == null || stages == null || stages.isEmpty()) {
            return;
        }

        CompanyRecruitmentStage offerStage = null;
        CompanyRecruitmentStage trialStage = null;
        CompanyRecruitmentStage offboardedStage = null;

        for (CompanyRecruitmentStage stage : stages) {
            if (stage.getStage() == ApplicationStage.OFFER_EXTENDED) {
                offerStage = stage;
            }
            if (stage.getStage() == ApplicationStage.TRIAL) {
                trialStage = stage;
            }
            if (stage.getStage() == ApplicationStage.OFFBOARDED) {
                offboardedStage = stage;
            }
        }

        if (offerStage != null && trialStage != null) {
            company.setOfferBeforeTrial(offerStage.getDisplayOrder() < trialStage.getDisplayOrder());
        }

        if (offboardedStage != null) {
            company.setEnableOffboardedStage(offboardedStage.isActive());
        }

        companyRepository.save(company);
    }

    private List<CompanyRecruitmentStage> buildFallbackStages(Company company) {
        boolean offerBeforeTrial = company.getOfferBeforeTrial() == null
                || Boolean.TRUE.equals(company.getOfferBeforeTrial());
        boolean enableOffboardedStage = Boolean.TRUE.equals(company.getEnableOffboardedStage());

        List<ApplicationStage> defaultOrder = ApplicationStage.getDefaultPipelineOrder(offerBeforeTrial);
        List<CompanyRecruitmentStage> stages = new ArrayList<>();
        int order = 1;
        for (ApplicationStage stage : defaultOrder) {
            CompanyRecruitmentStage setting = CompanyRecruitmentStage.builder()
                    .company(company)
                    .stage(stage)
                    .displayOrder(order++)
                    .build();

            if (stage == ApplicationStage.OFFBOARDED && !enableOffboardedStage) {
                setting.deactivate();
            }

            stages.add(setting);
        }

        return stages;
    }
}
