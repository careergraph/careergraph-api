package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.response.EmbeddingBatchResponse;
import com.hcmute.careergraph.persistence.dtos.response.EmbeddingSingleResponse;
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

    private final String embeddingUrl;
    private final String embeddingBatchUrl;
    private final String modelName;
    private final WebClient webClient;

    public HuggingFaceEmbeddingServiceImpl(
            @Value("${embedding.service.url:http://localhost:8000}") String embeddingServiceUrl,
            @Value("${embedding.service.model:}") String modelName) {
        this.embeddingUrl = embeddingServiceUrl + "/api/v1/embeddings";
        this.embeddingBatchUrl = embeddingServiceUrl + "/api/v1/embeddings/batch";
        this.modelName = modelName;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        if (modelName != null && !modelName.isBlank()) {
            body.put("model_name", modelName);
        }

        EmbeddingSingleResponse response = webClient.post()
                .uri(embeddingUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EmbeddingSingleResponse.class)
                .block();

        return toFloatArray(response.embedding());
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);
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

        return response.embeddings()
                .stream()
                .map(this::toFloatArray)
                .toList();
    }

    private float[] toFloatArray(List<?> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = ((Number) vector.get(i)).floatValue();
        }
        return arr;
    }
}
