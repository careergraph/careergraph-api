package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, String> {
}
