package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Contact;

import java.io.IOException;

public class ContactDeserializer extends StdDeserializer<Contact> {

    public ContactDeserializer() {
        this(null);
    }

    public ContactDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Contact deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        // Get from JSON node
        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        // Of contact
        String value = node.has("value") ? node.get("value").asText() : null;
        Boolean verified = node.has("verified") ? node.get("verified").asBoolean() : null;
        Boolean isPrimary = node.has("isPrimary") ? node.get("isPrimary").asBoolean() : null;

        Contact contact = Contact.builder()
                .value(value)
                .verified(verified)
                .isPrimary(isPrimary)
                .build();

        if (id != null) contact.setId(id);
        if (uuid != null) contact.setUUID(uuid);
        if (createDate != null) contact.setCreateDate(createDate);
        if (createdBy != null) contact.setCreatedBy(createdBy);

        return contact;
    }
}
