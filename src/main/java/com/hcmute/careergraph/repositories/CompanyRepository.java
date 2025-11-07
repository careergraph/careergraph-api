package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    /**
     * Find by tagname func
     * @param tagname string: Tagname of company
     * @return optional: Company
     */
    Optional<Company> findByTagname(String tagname);

    @Query(
            value = """
        SELECT
            c.id::text AS id,
            c.name AS name
        FROM companies c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """,
            nativeQuery = true
    )
    List<Object[]> lookup(@Param("query")  String query);
}
