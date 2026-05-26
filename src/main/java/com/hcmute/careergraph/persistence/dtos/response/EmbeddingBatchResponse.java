package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record EmbeddingBatchResponse(
    @JsonAlias({"model", "model_name"}) String modelName,
    List<List<Float>> embeddings,
    int dimensions,
    Integer count) {
}
