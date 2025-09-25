package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, String> {
}
