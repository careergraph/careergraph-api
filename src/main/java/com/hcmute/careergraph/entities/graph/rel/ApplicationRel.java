package com.hcmute.careergraph.entities.graph.rel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.deserialize.graph.rel.ApplicationRelDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import com.hcmute.careergraph.entities.graph.nodes.Job;
import com.hcmute.careergraph.enums.ApplicationStatus;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Data
@SuperBuilder
@RelationshipProperties
@JsonDeserialize(using = ApplicationRelDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationRel extends BaseGraph {

    private String candidateId;
    private String jobId;
    private String coverLetter;
    private String resumeUrl;
    private Integer rating;
    private String notes;
    private String appliedDate;
    private ApplicationStatus status;

    @TargetNode
    private Job job;
}
