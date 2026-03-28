package com.hcmute.careergraph.persistence.dtos.response;

import java.util.List;

/**
 * Legacy response kept for compatibility; prefer EmbeddingSingleResponse or
 * EmbeddingBatchResponse.
 */
public record EmbeddingResponse(List<List<Float>> embeddings) {
}
