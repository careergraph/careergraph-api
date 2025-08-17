package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.enums.ContactType;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.ContactDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@SuperBuilder
@Node(EntityLabels.Contact)
@JsonDeserialize(using = ContactDeserializer.class)
public class Contact extends BaseGraph {

    private String value;
    private Boolean verified;
    private Boolean isPrimary;
    private ContactType type;
}
