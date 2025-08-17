package com.hcmute.careergraph.deserialize.graph.rel;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.rel.ApplicationRel;
import com.hcmute.careergraph.enums.ApplicationStatus;

import java.io.IOException;

public class ApplicationRelDeserializer extends StdDeserializer<ApplicationRel> {

    public ApplicationRelDeserializer() {
        this(null);
    }

    public ApplicationRelDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public ApplicationRel deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        // BaseGraph fields
        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        // ApplicationRel specific fields
        String candidateId = node.has("candidateId") ? node.get("candidateId").asText() : null;
        String jobId = node.has("jobId") ? node.get("jobId").asText() : null;
        String coverLetter = node.has("coverLetter") ? node.get("coverLetter").asText() : null;
        String resumeUrl = node.has("resumeUrl") ? node.get("resumeUrl").asText() : null;
        Integer rating = node.has("rating") ? node.get("rating").asInt() : null;
        String notes = node.has("notes") ? node.get("notes").asText() : null;
        String appliedDate = node.has("appliedDate") ? node.get("appliedDate").asText() : null;

        // Handle ApplicationStatus enum
        ApplicationStatus status = null;
        if (node.has("status") && !node.get("status").isNull()) {
            String statusStr = node.get("status").asText();
            try {
                status = ApplicationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = null;
            }
        }

        // Build ApplicationRel using builder pattern
        ApplicationRel applicationRel = ApplicationRel.builder()
                .candidateId(candidateId)
                .jobId(jobId)
                .coverLetter(coverLetter)
                .resumeUrl(resumeUrl)
                .rating(rating)
                .notes(notes)
                .appliedDate(appliedDate)
                .status(status)
                .build();

        // Set BaseGraph fields using setters
        if (id != null) applicationRel.setId(id);
        if (uuid != null) applicationRel.setUUID(uuid);
        if (createDate != null) applicationRel.setCreateDate(createDate);
        if (createdBy != null) applicationRel.setCreatedBy(createdBy);

        return applicationRel;
    }
}