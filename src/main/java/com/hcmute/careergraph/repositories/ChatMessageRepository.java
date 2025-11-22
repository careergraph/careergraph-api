package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {


}
