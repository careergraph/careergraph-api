package com.hcmute.careergraph.entities.graph.rel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.deserialize.graph.rel.SkillRelDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import com.hcmute.careergraph.entities.graph.nodes.Skill;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Data
@SuperBuilder
@RelationshipProperties
@JsonDeserialize(using = SkillRelDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillRel extends BaseGraph {

    private String candidateId;     // If skill is required by Job -> candidateId = null
    private String jobId;           // If skill of candidate -> jobId = null
    private String skillId;
    private String proficiencyLevel;
    private Integer yearsOfExperience;
    private Boolean isVerified;
    private String endorsedBy;
    private Long endorsementDate;
    private Integer endorsementCount;

    @TargetNode
    private Skill skill;
}
