package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.SkillDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@SuperBuilder
@Node(EntityLabels.Skill)
@JsonDeserialize(using = SkillDeserializer.class)
public class Skill extends BaseGraph {

    private String name;
    private String category;
    private String description;
}
