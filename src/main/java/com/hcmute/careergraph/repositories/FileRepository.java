package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File,String> {
    List<File> findByOwnerId(String ownerId);

    List<File> findByOwnerIdAndStatusAndFileType(String ownerId, Status status, FileType fileType);

        Optional<File> findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
            String ownerId, Status status, List<FileType> fileTypes);
    
    // V2.1: Lấy TẤT CẢ CV có shareToFindJob = true
    List<File> findByOwnerIdAndStatusAndFileTypeInAndShareToFindJobOrderByCreatedDateDesc(
            String ownerId, Status status, List<FileType> fileTypes, Boolean shareToFindJob);
    
    Optional<File> findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
        String ownerId, Status status, List<FileType> fileTypes);
    Optional<File> findFirstByOwnerIdAndFilePathAndStatusOrderByCreatedDateDesc(
            String ownerId, String filePath, Status status);

    List<File> findByOwnerIdAndStatusAndFileTypeIn(String ownerId, Status status, List<FileType> fileTypes);

    List<File> findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
            String ownerId, Status status, List<FileType> fileTypes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update File f
               set f.shareToFindJob = :shareToFindJob
             where f.ownerId = :ownerId
               and f.status = :status
               and f.fileType in :fileTypes
            """)
    int updateShareToFindJobForCandidateResumes(
            @Param("ownerId") String ownerId,
            @Param("status") Status status,
            @Param("fileTypes") List<FileType> fileTypes,
            @Param("shareToFindJob") Boolean shareToFindJob);
}
