package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Connection;
import com.hcmute.careergraph.enums.candidate.ConnectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, String> {
    Optional<Connection> findByCandidateIdAndConnectedCompanyIdAndConnectionType(
	    String candidateId,
	    String connectedCompanyId,
	    ConnectionType connectionType
    );

    boolean existsByCandidateIdAndConnectedCompanyIdAndConnectionType(
	    String candidateId,
	    String connectedCompanyId,
	    ConnectionType connectionType
    );

    List<Connection> findAllByCandidateIdAndConnectionType(
	    String candidateId,
	    ConnectionType connectionType
    );
}
