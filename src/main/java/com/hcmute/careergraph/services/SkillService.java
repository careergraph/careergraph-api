package com.hcmute.careergraph.services;

import java.util.HashMap;
import java.util.List;

public interface SkillService {
    List<HashMap<String, String>> getLookupSkill(String query);
}
