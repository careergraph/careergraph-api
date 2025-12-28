package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File,String> {
    List<File> findByOwnerId(String ownerId);

    List<File> findByOwnerIdAndStatusAndFileType(String ownerId, Status status, FileType fileType);
}
