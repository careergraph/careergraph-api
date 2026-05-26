package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.response.EmbeddingBatchResponse;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HuggingFaceEmbeddingServiceImpl implements HuggingFaceEmbeddingService {

    private final String embeddingBatchUrl;
    private final String modelName;
    private final int expectedDimensions;
    private final WebClient webClient;

    public HuggingFaceEmbeddingServiceImpl(
            @Value("${embedding.service.url:http://localhost:8000}") String embeddingServiceUrl,
            @Value("${embedding.service.model:}") String modelName,
            @Value("${embedding.service.expected-dimensions:3072}") int expectedDimensions) {
        this.embeddingBatchUrl = embeddingServiceUrl + "/embed";
        this.modelName = modelName;
        this.expectedDimensions = expectedDimensions;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", texts);
        if (modelName != null && !modelName.isBlank()) {
            body.put("model_name", modelName);
        }

        EmbeddingBatchResponse response = webClient.post()
                .uri(embeddingBatchUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EmbeddingBatchResponse.class)
                .block();

        validateResponse(response, texts.size());

        return response.embeddings()
                .stream()
                .map(this::toFloatArray)
                .toList();
    }

    private void validateResponse(EmbeddingBatchResponse response, int requestedCount) {
        if (response == null) {
            throw new IllegalStateException("Embedding service returned no response body.");
        }
        if (response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("Embedding service returned no embeddings.");
        }
        if (response.embeddings().size() != requestedCount) {
            throw new IllegalStateException("Embedding service returned %d vectors for %d input texts."
                    .formatted(response.embeddings().size(), requestedCount));
        }
        if (response.dimensions() != expectedDimensions) {
            throw new IllegalStateException("Embedding service returned %d dimensions but Elasticsearch expects %d."
                    .formatted(response.dimensions(), expectedDimensions));
        }
    }

    private float[] toFloatArray(List<?> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = ((Number) vector.get(i)).floatValue();
        }
        return arr;
    }
}
