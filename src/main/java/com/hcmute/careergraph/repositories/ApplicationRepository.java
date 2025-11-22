package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.persistence.dtos.projection.AppliedJobsProjection;
import com.hcmute.careergraph.persistence.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    
    Page<Application> findByCandidateId(String candidateId, Pageable pageable);
    
    Page<Application> findByJobId(String jobId, Pageable pageable);

    @Query("""
        SELECT a
        FROM Application a
        WHERE a.job.id = :jobId
            AND a.job.company.id = :companyId
    """)
    List<Application> findByCompanyIdAndJobId(@Param("companyId") String companyId, @Param("jobId") String jobId);

    @Query("""
    SELECT a
    FROM Application a
    JOIN FETCH a.job j
    JOIN FETCH j.company
    WHERE a.candidate.id = :candidateId
     AND :status IS NULL OR a.currentStage = :status
""")
    Page<Application> getApplicationsByCandidateWithJobWithStatus(String candidateId, Pageable pageable, ApplicationStage status);

    @Query("""
        SELECT a
        FROM Application a
        JOIN FETCH a.job j
        JOIN FETCH j.company
        WHERE :status IS NULL OR a.currentStage = :status
    """)
    Page<Application> getAllApplicationsWithStatus( Pageable pageable, ApplicationStage status);

    @Query("""
        SELECT
            j.title          AS jobName,
            c.name           AS companyName,
            j.id             AS jobId,
            a.appliedDate    AS appliedAt,
            j.expiryDate     AS deadline,
            a.resumeUrl      AS linkResume,
            a.currentStage   AS status
        FROM Application a
        JOIN a.job j
        JOIN j.company c
        WHERE a.candidate.id = :candidateId
          AND (:status IS NULL OR a.currentStage = :status)
    """)
    Page<AppliedJobsProjection> findAppliedJobs(
            @Param("candidateId") String candidateId,
            @Param("status") ApplicationStage status,
            Pageable pageable
    );

    @Query("""
        SELECT
            j.title          AS jobName,
            c.name           AS companyName,
            j.id             AS jobId,
            a.appliedDate    AS appliedAt,
            j.expiryDate     AS deadline,
            a.resumeUrl      AS linkResume,
            a.currentStage   AS status
        FROM Application a
        JOIN a.job j
        JOIN j.company c
        WHERE (:status IS NULL OR a.currentStage = :status)
    """)
    Page<AppliedJobsProjection> findAppliedJobsAll(
            @Param("status") ApplicationStage status,
            Pageable pageable
    );


    boolean existsApplicationsByJobIdAndCandidateId(String jobId, String candidateId);
}
