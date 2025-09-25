package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);
}


