package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.CompanyDeserializer;
import com.hcmute.careergraph.entities.base.Party;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Data
@SuperBuilder
@Node(EntityLabels.Company)
@JsonDeserialize(using = CompanyDeserializer.class)
public class Company extends Party {

    private String size;
    private String website;
    private String ceoName;
    private int noOfMembers;
    private int yearFounded;

    @Relationship(type = EntityLabels.POST, direction = Relationship.Direction.INCOMING)
    private Job job;
}
