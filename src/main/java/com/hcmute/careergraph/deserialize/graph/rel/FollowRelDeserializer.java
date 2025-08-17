package com.hcmute.careergraph.deserialize.graph.rel;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.rel.ConnectionRel;
import com.hcmute.careergraph.entities.graph.rel.FollowRel;
import com.hcmute.careergraph.enums.ConnectionType;
import com.hcmute.careergraph.enums.PartyType;

import java.io.IOException;

public class FollowRelDeserializer extends StdDeserializer<FollowRel> {

    public FollowRelDeserializer() {
        this(null);
    }

    public FollowRelDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public FollowRel deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        String partyId = node.has("partyId") ? node.get("partyId").asText() : null;
        String candidateId = node.has("candidateId") ? node.get("candidateId").asText() : null;
        Boolean hasSeen = node.has("hasSeen") ? node.get("hasSeen").asBoolean() : null;
        Boolean disableNotification = node.has("disableNotification") ? node.get("disableNotification").asBoolean() : null;
        String note = node.has("note") ? node.get("note").asText() : null;

        PartyType partyType = null;
        if (node.has("partyType") && !node.get("partyType").isNull()) {
            String partyTypeStr = node.get("partyType").asText();
            try {
                partyType = PartyType.valueOf(partyTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                partyType = null;
            }
        }

        ConnectionType connectionType = null;
        if (node.has("connectionType") && !node.get("connectionType").isNull()) {
            String connectionTypeStr = node.get("connectionType").asText();
            try {
                connectionType = ConnectionType.valueOf(connectionTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                connectionType = null;
            }
        }

        FollowRel followRel = FollowRel.builder()
                .partyId(partyId)
                .candidateId(candidateId)
                .partyType(partyType)
                .connectionType(connectionType)
                .note(note)
                .build();

        if (id != null) followRel.setId(id);
        if (uuid != null) followRel.setUUID(uuid);
        if (createDate != null) followRel.setCreateDate(createDate);
        if (createdBy != null) followRel.setCreatedBy(createdBy);

        return followRel;
    }
}