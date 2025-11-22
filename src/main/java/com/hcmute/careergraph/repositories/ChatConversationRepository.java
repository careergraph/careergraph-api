package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {


}
