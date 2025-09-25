package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, String> {
}
