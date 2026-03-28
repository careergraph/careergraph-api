package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingSingleResponse(
    @JsonProperty("model_name") String modelName,
    List<Float> embedding,
    int dimensions) {
}
