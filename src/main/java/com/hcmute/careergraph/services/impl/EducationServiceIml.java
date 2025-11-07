package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.repositories.EducationRepository;
import com.hcmute.careergraph.services.EducationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EducationServiceIml implements EducationService {

    private  final EducationRepository educationRepository;

    @Override
    public List<HashMap<String, String>> getLookupEducation(String query) {
        List<Object[]> list = educationRepository.lookup(query);
        List<HashMap<String, String>> result = new ArrayList<>();
        for (Object[] row : list) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", row[0].toString());
            map.put("name", row[1].toString());
            result.add(map);
        }
        return result;
    }
}
