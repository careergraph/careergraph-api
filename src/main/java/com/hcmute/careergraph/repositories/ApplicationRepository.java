package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
}
