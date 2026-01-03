package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("""
        SELECT CONCAT(cm.role, ': ', cm.content)
        FROM ChatMessage cm
        WHERE cm.chatConversation.id = :conversationId
        ORDER BY cm.createdDate DESC
        LIMIT 3
    """)
    List<String> findLastFormattedMessage(@Param("conversationId") String conversationId);

    /**
     * Xóa tất cả messages của conversation (cascade delete)
     */
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.chatConversation.id = :conversationId")
    void deleteByConversationId(@Param("conversationId") String conversationId);
}
