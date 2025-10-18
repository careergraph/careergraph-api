package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.repositories.SkillRepository;
import com.hcmute.careergraph.services.ScraperDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ScraperDataServiceImpl implements ScraperDataService {

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;


    @Override
    public void importJobFromFile(String filePath) {

    }
}
