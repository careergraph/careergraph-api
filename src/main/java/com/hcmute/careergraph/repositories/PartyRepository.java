package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRepository extends JpaRepository<Party, String> {
}
