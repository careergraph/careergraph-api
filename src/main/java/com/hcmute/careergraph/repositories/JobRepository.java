package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.EducationType;
import com.hcmute.careergraph.enums.job.EmploymentType;
import com.hcmute.careergraph.enums.job.ExperienceLevel;
import com.hcmute.careergraph.enums.job.JobCategory;
import com.hcmute.careergraph.persistence.models.Job;
import org.joda.time.DateTime;
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

    @Query("""
        SELECT j
        FROM Job j
        WHERE j.company.id = :companyId
    """)
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
    Page<Job> searchJobForCompany(
            @Param("companyId") String companyId,
            @Param("statuses") List<Status> statuses,
            @Param("categories") List<JobCategory> categories,
            @Param("types") List<EmploymentType> types,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
        SELECT j
        FROM Job j
        WHERE (NULLIF(TRIM(:city), '') IS NULL
                OR lower(j.city) LIKE lower(concat('%', :city, '%'))
                OR lower(j.state) LIKE lower(concat('%', :city, '%')))
            AND (:jobCategories IS NULL OR j.jobCategory IN :jobCategories)
            AND (:employmentTypes IS NULL OR j.employmentType IN :employmentTypes)
            AND (:experienceLevels IS NULL OR j.experienceLevel IN :experienceLevels)
            AND (:educationTypes IS NULL OR j.education IN :educationTypes)
            AND (NULLIF(TRIM(:query), '') IS NULL OR
                lower(j.title) LIKE lower(concat('%', :query, '%')) OR
                lower(j.description) LIKE lower(concat('%', :query, '%')))
    """)
    Page<Job> searchJobForCandidate(
            @Param("candidateId") String candidateId,
            @Param("city") String city,
            @Param("jobCategories") List<JobCategory> jobCategories,
            @Param("employmentTypes") List<EmploymentType> employmentTypes,
            @Param("experienceLevels") List<ExperienceLevel> experienceLevels,
            @Param("educationTypes") List<EducationType> educationTypes,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'ACTIVE'
        ORDER BY j.views DESC, j.applicants DESC, j.liked DESC, j.shared DESC
    """)
    List<Job> findPopularJob();

    @Query(value = """
        SELECT DISTINCT j.*
        FROM jobs j
        WHERE j.status = 'ACTIVE'
            AND j.expiry_date >= :currentDate
            AND EXISTS (
                SELECT 1
                FROM candidates c
                WHERE c.id = :userId AND (
                    (c.industries IS NOT NULL AND jsonb_exists(c.industries::jsonb, j.job_category))
                    OR (c.locations IS NOT NULL AND jsonb_exists(c.locations::jsonb, j.state))
                    OR (c.work_types IS NOT NULL AND jsonb_exists(c.work_types::jsonb, j.employment_type))
                    OR (c.years_of_experience IS NOT NULL
                        AND (j.min_experience IS NULL OR c.years_of_experience >= j.min_experience)
                        AND (j.max_experience IS NULL OR c.years_of_experience <= j.max_experience))
                    OR (c.education_level IS NOT NULL AND c.education_level = j.education)
                )
          )
    """, nativeQuery = true)
    List<Job> findJobByPersonalized(@Param("userId") String userId,
                                    @Param("currentDate") String currentDate);

    List<Job> findAllByOrderByCreatedDateDesc(Pageable pageable);
}
