package com.hcmute.careergraph.entities.graph.nodes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.deserialize.graph.nodes.CandidateDeserializer;
import com.hcmute.careergraph.entities.base.Party;
import com.hcmute.careergraph.entities.graph.rel.*;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Data
@SuperBuilder
@Node(EntityLabels.Candidate)
@JsonDeserialize(using = CandidateDeserializer.class)
public class Candidate extends Party {

    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String gender;
    private String currentJobTitle;
    private String currentCompany;
    private String industry;
    private Integer yearsOfExperience;
    private String workLocation;
    private Boolean isOpenToWork;
    private String summary;
    private String resume;

    @Relationship(type = EntityLabels.CONNECTED)
    private Set<ConnectionRel> connections;

    @Relationship(type = EntityLabels.STUDIED, direction = Relationship.Direction.OUTGOING)
    private Set<WorkRel> educations;

    @Relationship(type = EntityLabels.WORKED, direction = Relationship.Direction.OUTGOING)
    private Set<WorkRel> experiences;

    @Relationship(type = EntityLabels.HAS_SKILL, direction = Relationship.Direction.OUTGOING)
    private Set<SkillRel> skills;

    @Relationship(type = EntityLabels.FOLLOWS, direction = Relationship.Direction.OUTGOING)
    private Set<FollowRel> following;

    @Relationship(type = EntityLabels.FOLLOWS, direction = Relationship.Direction.INCOMING)
    private Set<FollowRel> followers;

    @Relationship(type = EntityLabels.APPLIED, direction = Relationship.Direction.OUTGOING)
    private Set<ApplicationRel> applications;
}
