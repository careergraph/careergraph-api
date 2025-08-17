package com.hcmute.careergraph.entities.graph.rel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.deserialize.graph.rel.WorkRelDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import com.hcmute.careergraph.entities.base.Party;
import com.hcmute.careergraph.enums.PartyType;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Data
@SuperBuilder
@RelationshipProperties
@JsonDeserialize(using = WorkRelDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkRel extends BaseGraph {

    private String candidateId;
    private String partyId;
    private String startDate;
    private String endDate;
    private Integer salary;
    private String partyTitle;  // Student at Education and Job title at Company
    private Boolean isCurrent;
    private String description;
    private PartyType partyType;

    @TargetNode
    private Party education;
}
