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
        SELECT TOP(7) 
            CAST(c.id AS VARCHAR) AS id, 
            c.name AS name
        FROM company c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """,
            nativeQuery = true
    )
    List<HashMap<String, String>> lookup(@Param("query")  String query);
}
