package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.EducationDeserializer;
import com.hcmute.careergraph.entities.base.Party;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@SuperBuilder
@Node(EntityLabels.Education)
@JsonDeserialize(using = EducationDeserializer.class)
public class Education extends Party {

    private String startDate;
    private String endDate;
    private String description;
    private Boolean isCurrentlyStudying;
}
