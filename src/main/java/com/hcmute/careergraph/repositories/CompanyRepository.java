package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    /**
     * Find by tagname func
     * @param tagname string: Tagname of company
     * @return optional: Company
     */
    Optional<Company> findByTagname(String tagname);
}
