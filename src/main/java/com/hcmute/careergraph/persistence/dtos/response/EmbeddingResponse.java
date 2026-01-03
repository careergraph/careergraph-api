package com.hcmute.careergraph.persistence.dtos.response;

import java.util.List;

public record EmbeddingResponse(List<List<Float>> embeddings) {}
