package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.persistence.models.CompanyVerificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CompanyVerificationRequestRepository extends JpaRepository<CompanyVerificationRequest, String> {

    Optional<CompanyVerificationRequest> findTopByCompanyIdOrderBySubmittedAtDescCreatedDateDesc(String companyId);

    java.util.List<CompanyVerificationRequest> findByCompanyIdOrderByCreatedDateDesc(String companyId);

    @Query(
            value = """
                    SELECT cvr.*
                    FROM company_verification_requests cvr
                    JOIN companies c ON c.id = cvr.company_id
                    LEFT JOIN accounts submitted_by ON submitted_by.id = cvr.submitted_by_account_id
                    WHERE (:statusName IS NULL OR cvr.verification_status = CAST(:statusName AS varchar))
                      AND (
                            :query IS NULL
                            OR c.name ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR cvr.company_name ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR cvr.tax_code ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR submitted_by.email ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                          )
                    ORDER BY COALESCE(cvr.submitted_at, cvr.created_date) DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM company_verification_requests cvr
                    JOIN companies c ON c.id = cvr.company_id
                    LEFT JOIN accounts submitted_by ON submitted_by.id = cvr.submitted_by_account_id
                    WHERE (:statusName IS NULL OR cvr.verification_status = CAST(:statusName AS varchar))
                      AND (
                            :query IS NULL
                            OR c.name ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR cvr.company_name ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR cvr.tax_code ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                            OR submitted_by.email ILIKE CONCAT('%%', CAST(:query AS text), '%%')
                          )
                    """,
            nativeQuery = true
    )
    Page<CompanyVerificationRequest> searchForAdmin(
            @Param("statusName") String statusName,
            @Param("query") String query,
            Pageable pageable);

    long countByVerificationStatus(CompanyVerificationStatus status);

    long countByReviewedAtBetween(LocalDateTime from, LocalDateTime to);
}
