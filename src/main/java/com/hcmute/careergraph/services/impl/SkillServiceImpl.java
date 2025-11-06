package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.models.Skill;
import com.hcmute.careergraph.repositories.SkillRepository;
import com.hcmute.careergraph.services.SkillService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    @Override
    public List<HashMap<String, String>> getLookupSkill(String query) {
        List<Skill> list = skillRepository.lookupSkill(query);
        List<HashMap<String, String>> result = new ArrayList<>();
        for (Skill row : list) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", row.getId());
            map.put("name", row.getName());
            result.add(map);
        }
        return result;
    }
}
