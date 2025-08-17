package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Job;
import com.hcmute.careergraph.entities.graph.rel.SkillRel;
import com.hcmute.careergraph.enums.EmploymentType;
import com.hcmute.careergraph.enums.JobStatus;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class JobDeserializer extends StdDeserializer<Job> {

    public JobDeserializer() {
        this(null);
    }

    public JobDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Job deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Job.JobBuilder<?, ?> builder = Job.builder();
        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;


        if (node.has("title")) {
            builder.title(node.get("title").asText());
        }
        if (node.has("description")) {
            builder.description(node.get("description").asText());
        }
        if (node.has("requirements")) {
            builder.requirements(node.get("requirements").asText());
        }
        if (node.has("benefits")) {
            builder.benefits(node.get("benefits").asText());
        }
        if (node.has("salaryRange")) {
            builder.salaryRange(node.get("salaryRange").asText());
        }
        if (node.has("experienceLevel")) {
            builder.experienceLevel(node.get("experienceLevel").asText());
        }
        if (node.has("workArrangement")) {
            builder.workArrangement(node.get("workArrangement").asText());
        }
        if (node.has("postedDate")) {
            builder.postedDate(node.get("postedDate").asText());
        }
        if (node.has("expiryDate")) {
            builder.expiryDate(node.get("expiryDate").asText());
        }
        if (node.has("numberOfPositions")) {
            builder.numberOfPositions(node.get("numberOfPositions").asInt());
        }
        if (node.has("workLocation")) {
            builder.workLocation(node.get("workLocation").asText());
        }
        if (node.has("isUrgent")) {
            builder.isUrgent(node.get("isUrgent").asBoolean());
        }

        // Enum fields
        if (node.has("employmentType")) {
            String employmentTypeStr = node.get("employmentType").asText();
            try {
                builder.employmentType(EmploymentType.valueOf(employmentTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle invalid enum value - either log warning or set default
                builder.employmentType(null);
            }
        }

        if (node.has("status")) {
            String statusStr = node.get("status").asText();
            try {
                builder.status(JobStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle invalid enum value - either log warning or set default
                builder.status(JobStatus.OPEN); // Default status
            }
        }

        if (id != null) builder.id(id);
        if (uuid != null) builder.uuid(uuid);
        if (createDate != null) builder.createDate(createDate);
        if (createdBy != null) builder.createdBy(createdBy);

        // Handle requiredSkills relationship
        if (node.has("requiredSkills")) {
            JsonNode skillsNode = node.get("requiredSkills");
            if (skillsNode.isArray()) {
                Set<SkillRel> skillRels = new HashSet<>();
                for (JsonNode skillNode : skillsNode) {
                    try {
                        SkillRel skillRel = jp.getCodec().treeToValue(skillNode, SkillRel.class);
                        if (skillRel != null) {
                            skillRels.add(skillRel);
                        }
                    } catch (Exception e) {
                        // Log error and continue processing other skills
                        // logger.warn("Failed to deserialize skill relationship: {}", e.getMessage());
                    }
                }
                builder.requiredSkills(skillRels);
            }
        }

        return builder.build();
    }
}
