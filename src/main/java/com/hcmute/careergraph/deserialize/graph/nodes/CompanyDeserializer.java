package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Company;
import com.hcmute.careergraph.entities.graph.nodes.Contact;
import com.hcmute.careergraph.entities.graph.nodes.Address;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CompanyDeserializer extends StdDeserializer<Company> {

    public CompanyDeserializer() {
        this(null);
    }

    public CompanyDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Company deserialize(JsonParser jp, DeserializationContext dt)
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

        String size = node.has("size") ? node.get("size").asText() : null;
        String website = node.has("website") ? node.get("website").asText() : null;
        String ceoName = node.has("ceoName") ? node.get("ceoName").asText() : null;
        Integer noOfMembers = node.has("noOfMembers") ? node.get("noOfMembers").asInt() : 0;
        Integer yearFounded = node.has("yearFounded") ? node.get("yearFounded").asInt() : 0;

        Company company = Company.builder()
                .name(name)
                .tagname(tagname)
                .avatar(avatar)
                .cover(cover)
                .noOfFollowers(noOfFollowers)
                .noOfFollowing(noOfFollowing)
                .noOfConnections(noOfConnections)
                .size(size)
                .website(website)
                .ceoName(ceoName)
                .noOfMembers(noOfMembers)
                .yearFounded(yearFounded)
                .build();

        if (id != null) company.setId(id);
        if (uuid != null) company.setUUID(uuid);
        if (createDate != null) company.setCreateDate(createDate);
        if (createdBy != null) company.setCreatedBy(createdBy);

        // Handle relationships
        if (node.has("contacts")) {
            Set<Contact> contacts = new HashSet<>();
            // TODO: Deserialize contacts array if needed
            company.setContacts(contacts);
        }

        if (node.has("addresses")) {
            Set<Address> addresses = new HashSet<>();
            // TODO: Deserialize addresses array if needed
            company.setAddresses(addresses);
        }

        return company;
    }
}