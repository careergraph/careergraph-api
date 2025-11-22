package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File,String> {
}
