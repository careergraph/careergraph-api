package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Education;
import com.hcmute.careergraph.entities.graph.nodes.Contact;
import com.hcmute.careergraph.entities.graph.nodes.Address;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class EducationDeserializer extends StdDeserializer<Education> {

    public EducationDeserializer() {
        this(null);
    }

    public EducationDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Education deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        String name = node.has("name") ? node.get("name").asText() : null;
        String tagname = node.has("tagname") ? node.get("tagname").asText() : null;
        String avatar = node.has("avatar") ? node.get("avatar").asText() : null;
        String cover = node.has("cover") ? node.get("cover").asText() : null;
        Integer noOfFollowers = node.has("noOfFollowers") ? node.get("noOfFollowers").asInt() : 0;
        Integer noOfFollowing = node.has("noOfFollowing") ? node.get("noOfFollowing").asInt() : 0;
        Integer noOfConnections = node.has("noOfConnections") ? node.get("noOfConnections").asInt() : 0;

        String startDate = node.has("startDate") ? node.get("startDate").asText() : null;
        String endDate = node.has("endDate") ? node.get("endDate").asText() : null;
        String description = node.has("description") ? node.get("description").asText() : null;
        Boolean isCurrentlyStudying = node.has("isCurrentlyStudying") ? node.get("isCurrentlyStudying").asBoolean() : null;

        Education education = Education.builder()
                .name(name)
                .tagname(tagname)
                .avatar(avatar)
                .cover(cover)
                .noOfFollowers(noOfFollowers)
                .noOfFollowing(noOfFollowing)
                .noOfConnections(noOfConnections)
                .startDate(startDate)
                .endDate(endDate)
                .description(description)
                .isCurrentlyStudying(isCurrentlyStudying)
                .build();

        if (id != null) education.setId(id);
        if (uuid != null) education.setUUID(uuid);
        if (createDate != null) education.setCreateDate(createDate);
        if (createdBy != null) education.setCreatedBy(createdBy);

        // Handle relationships
        if (node.has("contacts")) {
            Set<Contact> contacts = new HashSet<>();
            // TODO: Deserialize contacts array if needed
            education.setContacts(contacts);
        }

        if (node.has("addresses")) {
            Set<Address> addresses = new HashSet<>();
            // TODO: Deserialize addresses array if needed
            education.setAddresses(addresses);
        }

        return education;
    }
}