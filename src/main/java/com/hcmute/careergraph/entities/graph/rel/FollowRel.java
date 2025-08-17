package com.hcmute.careergraph.entities.graph.rel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.deserialize.graph.rel.ConnectionRelDeserializer;
import com.hcmute.careergraph.deserialize.graph.rel.FollowRelDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import com.hcmute.careergraph.entities.base.Party;
import com.hcmute.careergraph.enums.ConnectionType;
import com.hcmute.careergraph.enums.PartyType;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Data
@SuperBuilder
@RelationshipProperties
@JsonDeserialize(using = FollowRelDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FollowRel extends BaseGraph {

    private String partyId;
    private String candidateId;
    private String note;
    private PartyType partyType;
    private ConnectionType connectionType;

    @TargetNode
    private Party party;
}
