package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, String> {
}
