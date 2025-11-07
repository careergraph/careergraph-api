package com.hcmute.careergraph.services;

import java.util.HashMap;
import java.util.List;

public interface EducationService {
    List<HashMap<String, String>> getLookupEducation(String query);
}
