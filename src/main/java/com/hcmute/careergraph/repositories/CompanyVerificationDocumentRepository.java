package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.CompanyVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyVerificationDocumentRepository extends JpaRepository<CompanyVerificationDocument, String> {
}
