package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.AddressDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@SuperBuilder
@Node(EntityLabels.Address)
@JsonDeserialize(using = AddressDeserializer.class)
public class Address extends BaseGraph {

    private String name;
    private String country;
    private String province;
    private String district;
    private String ward;
    private Boolean isPrimary;
}
