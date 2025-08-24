package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.JobDeserializer;
import com.hcmute.careergraph.entities.base.BaseGraph;
import com.hcmute.careergraph.entities.graph.rel.SkillRel;
import com.hcmute.careergraph.enums.EmploymentType;
import com.hcmute.careergraph.enums.JobStatus;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Data
@SuperBuilder
@Node(EntityLabels.Job)
@JsonDeserialize(using = JobDeserializer.class)
public class Job extends BaseGraph {

    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private String salaryRange;
    private String experienceLevel;
    private String workArrangement;
    private String postedDate;
    private String expiryDate;
    private Integer numberOfPositions;
    private String workLocation;
    private EmploymentType employmentType;
    private JobStatus status;
    private Boolean isUrgent;

    @Relationship(type = EntityLabels.REQUIRES_SKILL, direction = Relationship.Direction.OUTGOING)
    private Set<SkillRel> requiredSkills;
}
