package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Candidate;
import com.hcmute.careergraph.entities.graph.nodes.Contact;
import com.hcmute.careergraph.entities.graph.nodes.Address;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CandidateDeserializer extends StdDeserializer<Candidate> {

    public CandidateDeserializer() {
        this(null);
    }

    public CandidateDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Candidate deserialize(JsonParser jp, DeserializationContext dt)
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

        String firstName = node.has("firstName") ? node.get("firstName").asText() : null;
        String lastName = node.has("lastName") ? node.get("lastName").asText() : null;
        String dateOfBirth = node.has("dateOfBirth") ? node.get("dateOfBirth").asText() : null;
        String gender = node.has("gender") ? node.get("gender").asText() : null;
        String currentJobTitle = node.has("currentJobTitle") ? node.get("currentJobTitle").asText() : null;
        String currentCompany = node.has("currentCompany") ? node.get("currentCompany").asText() : null;
        String industry = node.has("industry") ? node.get("industry").asText() : null;
        Integer yearsOfExperience = node.has("yearsOfExperience") ? node.get("yearsOfExperience").asInt() : null;
        String workLocation = node.has("workLocation") ? node.get("workLocation").asText() : null;
        Boolean isOpenToWork = node.has("isOpenToWork") ? node.get("isOpenToWork").asBoolean() : null;
        String summary = node.has("summary") ? node.get("summary").asText() : null;
        String resume = node.has("resume") ? node.get("resume").asText() : null;

        Candidate candidate = Candidate.builder()
                .name(name)
                .tagname(tagname)
                .avatar(avatar)
                .cover(cover)
                .noOfFollowers(noOfFollowers)
                .noOfFollowing(noOfFollowing)
                .noOfConnections(noOfConnections)
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .currentJobTitle(currentJobTitle)
                .currentCompany(currentCompany)
                .industry(industry)
                .yearsOfExperience(yearsOfExperience)
                .workLocation(workLocation)
                .isOpenToWork(isOpenToWork)
                .summary(summary)
                .resume(resume)
                .build();

        if (id != null) candidate.setId(id);
        if (uuid != null) candidate.setUUID(uuid);
        if (createDate != null) candidate.setCreateDate(createDate);
        if (createdBy != null) candidate.setCreatedBy(createdBy);

        // Handle relationships
        if (node.has("contacts")) {
            Set<Contact> contacts = new HashSet<>();
            // TODO: Deserialize contacts array if needed
            candidate.setContacts(contacts);
        }

        if (node.has("addresses")) {
            Set<Address> addresses = new HashSet<>();
            // TODO: Deserialize addresses array if needed
            candidate.setAddresses(addresses);
        }

        return candidate;
    }
}