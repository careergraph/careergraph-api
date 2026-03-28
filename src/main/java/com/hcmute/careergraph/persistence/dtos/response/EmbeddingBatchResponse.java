package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingBatchResponse(
    @JsonProperty("model_name") String modelName,
    List<List<Float>> embeddings,
    int dimensions,
    int count) {
}
