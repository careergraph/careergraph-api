package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File,String> {
    List<File> findByOwnerId(String ownerId);

    List<File> findByOwnerIdAndStatusAndFileType(String ownerId, Status status, FileType fileType);

        Optional<File> findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
            String ownerId, Status status, List<FileType> fileTypes);

    Optional<File> findFirstByOwnerIdAndFilePathAndStatusOrderByCreatedDateDesc(
            String ownerId, String filePath, Status status);
}
