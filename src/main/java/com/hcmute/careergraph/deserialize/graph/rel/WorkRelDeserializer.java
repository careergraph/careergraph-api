package com.hcmute.careergraph.deserialize.graph.rel;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.rel.WorkRel;
import com.hcmute.careergraph.enums.PartyType;

import java.io.IOException;

public class WorkRelDeserializer extends StdDeserializer<WorkRel> {

    public WorkRelDeserializer() {
        this(null);
    }

    public WorkRelDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public WorkRel deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        String candidateId = node.has("candidateId") ? node.get("candidateId").asText() : null;
        String educationId = node.has("educationId") ? node.get("educationId").asText() : null;
        String startDate = node.has("startDate") ? node.get("startDate").asText() : null;
        String endDate = node.has("endDate") ? node.get("endDate").asText() : null;
        String partyTitle = node.has("partyTitle") ? node.get("partyTitle").asText() : null;
        Integer salary = node.has("salary") ? node.get("salary").asInt() : null;
        Boolean isCurrent = node.has("isCurrent") ? node.get("isCurrent").asBoolean() : null;
        String description = node.has("description") ? node.get("description").asText() : null;
        PartyType partyType = node.has("partyType") ? PartyType.valueOf(node.get("partyType").asText()) : null;

        WorkRel studyRel = WorkRel.builder()
                .candidateId(candidateId)
                .partyId(educationId)
                .startDate(startDate)
                .endDate(endDate)
                .partyTitle(partyTitle)
                .salary(salary)
                .isCurrent(isCurrent)
                .description(description)
                .partyType(partyType)
                .build();

        if (id != null) studyRel.setId(id);
        if (uuid != null) studyRel.setUUID(uuid);
        if (createDate != null) studyRel.setCreateDate(createDate);
        if (createdBy != null) studyRel.setCreatedBy(createdBy);

        return studyRel;
    }
}