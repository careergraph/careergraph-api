package com.hcmute.careergraph.deserialize.graph.rel;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.rel.SkillRel;

import java.io.IOException;

public class SkillRelDeserializer extends StdDeserializer<SkillRel> {

    public SkillRelDeserializer() {
        this(null);
    }

    public SkillRelDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public SkillRel deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        String candidateId = node.has("candidateId") ? node.get("candidateId").asText() : null;
        String skillId = node.has("skillId") ? node.get("skillId").asText() : null;
        String proficiencyLevel = node.has("proficiencyLevel") ? node.get("proficiencyLevel").asText() : null;
        Integer yearsOfExperience = node.has("yearsOfExperience") ? node.get("yearsOfExperience").asInt() : null;
        Boolean isVerified = node.has("isVerified") ? node.get("isVerified").asBoolean() : null;
        String endorsedBy = node.has("endorsedBy") ? node.get("endorsedBy").asText() : null;
        Long endorsementDate = node.has("endorsementDate") ? node.get("endorsementDate").asLong() : null;
        Integer endorsementCount = node.has("endorsementCount") ? node.get("endorsementCount").asInt() : null;

        SkillRel skillRel = SkillRel.builder()
                .candidateId(candidateId)
                .skillId(skillId)
                .proficiencyLevel(proficiencyLevel)
                .yearsOfExperience(yearsOfExperience)
                .isVerified(isVerified)
                .endorsedBy(endorsedBy)
                .endorsementDate(endorsementDate)
                .endorsementCount(endorsementCount)
                .build();

        if (id != null) skillRel.setId(id);
        if (uuid != null) skillRel.setUUID(uuid);
        if (createDate != null) skillRel.setCreateDate(createDate);
        if (createdBy != null) skillRel.setCreatedBy(createdBy);

        return skillRel;
    }
}
