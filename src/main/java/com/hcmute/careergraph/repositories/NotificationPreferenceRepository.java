package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
}
