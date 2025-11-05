package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyById(String companyId) {

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    @Override
    public List<HashMap<String, String>> lookup(String query) {
        List<Object[]> raw = companyRepository.lookup(query);
        List<HashMap<String, String>> result = new ArrayList<>();
        for (Object[] row : raw) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(row[0]));
            map.put("name", String.valueOf(row[1]));
            result.add(map);
        }
        return result;
    }
}
