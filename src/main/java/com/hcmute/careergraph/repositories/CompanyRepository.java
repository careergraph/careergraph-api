package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;

import java.util.Collection;
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

    @Query(
            value = """
                    SELECT
                        c.id AS company_id,
                        c.name AS company_name,
                        c.tax_code AS tax_code,
                        a.email AS hr_email,
                        c.verification_status AS verification_status,
                        c.operational_status AS operational_status,
                        c.verification_submitted_at AS submitted_at,
                        COUNT(cvr.id) AS total_requests
                    FROM companies c
                    JOIN parties p ON p.id = c.id
                    LEFT JOIN accounts a ON a.company_id = c.id
                    LEFT JOIN company_verification_requests cvr ON cvr.company_id = c.id
                    WHERE (:verificationStatus IS NULL OR c.verification_status = CAST(:verificationStatus AS varchar))
                      AND (:operationalStatus IS NULL OR c.operational_status = CAST(:operationalStatus AS varchar))
                      AND (
                            :query IS NULL
                            OR c.name ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR c.tax_code ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR a.email ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                          )
                    GROUP BY
                        c.id, c.name, c.tax_code, a.email,
                        c.verification_status, c.operational_status,
                        c.verification_submitted_at, p.created_date
                    ORDER BY COALESCE(c.verification_submitted_at, p.created_date) DESC, p.created_date DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM companies c
                    JOIN parties p ON p.id = c.id
                    LEFT JOIN accounts a ON a.company_id = c.id
                    WHERE (:verificationStatus IS NULL OR c.verification_status = CAST(:verificationStatus AS varchar))
                      AND (:operationalStatus IS NULL OR c.operational_status = CAST(:operationalStatus AS varchar))
                      AND (
                            :query IS NULL
                            OR c.name ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR c.tax_code ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR a.email ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                          )
                    """,
            nativeQuery = true
    )
    Page<Object[]> searchCompaniesForAdmin(
            @Param("verificationStatus") String verificationStatus,
            @Param("operationalStatus") String operationalStatus,
            @Param("query") String query,
            Pageable pageable);

    long countByOperationalStatusIn(Collection<CompanyOperationalStatus> statuses);
}
