package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.EmploymentType;
import com.hcmute.careergraph.enums.job.JobCategory;
import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByCompanyId(String companyId);

    Page<Job> findByCompanyId(String companyId, Pageable pageable);

    Page<Job> findByJobCategory(JobCategory jobCategory, Pageable pageable);

    @Query("""
        SELECT j.id, j.title
        FROM Job j
        WHERE j.company.id = :companyId
            AND lower(j.title) LIKE lower(concat('%', :query,'%'))
    """)
    Map<String, String> lookup(@Param("companyId") String companyId, @Param("query") String query);

    @Query("""
        SELECT j FROM Job j WHERE j.id = :jobId AND j.company.id = :companyId
    """)
    Optional<Job> findByIdAndCompanyId(@Param("jobId") String jobId, @Param("companyId") String companyId);

    @Query("""
        SELECT j
        FROM Job j
        WHERE j.company.id = :companyId
            AND (:statuses IS NULL OR j.status IN :statuses)
            AND (:categories IS NULL OR j.jobCategory IN :categories)
            AND (:types IS NULL OR j.employmentType IN :types)
            AND (
                :query IS NULL OR
                lower(j.title) LIKE lower(concat('%', :query, '%')) OR
                lower(j.description) LIKE lower(concat('%', :query, '%')))
    """)
    Page<Job> search(
            @Param("companyId") String companyId,
            @Param("statuses") List<Status> status,
            @Param("categories") List<JobCategory> categories,
            @Param("types") List<EmploymentType> types,
            @Param("query") String query,
            Pageable pageable
    );
}
