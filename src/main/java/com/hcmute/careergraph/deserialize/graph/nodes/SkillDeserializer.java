package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Skill;

import java.io.IOException;

public class SkillDeserializer extends StdDeserializer<Skill> {

    public SkillDeserializer() {
        this(null);
    }

    public SkillDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Skill deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        String name = node.has("name") ? node.get("name").asText() : null;
        String category = node.has("category") ? node.get("category").asText() : null;
        String description = node.has("description") ? node.get("description").asText() : null;

        Skill skill = Skill.builder()
                .name(name)
                .category(category)
                .description(description)
                .build();

        if (id != null) skill.setId(id);
        if (uuid != null) skill.setUUID(uuid);
        if (createDate != null) skill.setCreateDate(createDate);
        if (createdBy != null) skill.setCreatedBy(createdBy);

        return skill;
    }
}