package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.persistence.models.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {

    @Query("""
        SELECT cc
        FROM ChatConversation cc
        WHERE cc.partyId = :partyId AND cc.partyType = :partyType
    """)
    List<ChatConversation> findAllByPartyIdAndPartyType(@Param("partyId") String partyId,
                                                        @Param("partyType") PartyType partyType);

    @Query("""
        SELECT cc
        FROM ChatConversation cc
        WHERE cc.partyId = :partyId AND cc.partyType = :partyType
        ORDER BY cc.createdDate DESC LIMIT 1
    """)
    Optional<ChatConversation> findLastedConversation(@Param("partyId") String partyId,
                                                      @Param("partyType") PartyType partyType);
}
