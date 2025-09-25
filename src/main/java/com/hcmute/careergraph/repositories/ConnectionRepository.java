package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRepository extends JpaRepository<Connection, String> {
}
