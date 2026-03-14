package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.response.EmbeddingResponse;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class HuggingFaceEmbeddingServiceImpl implements HuggingFaceEmbeddingService {

    private final String embeddingUrl;
    private final WebClient webClient;

    public HuggingFaceEmbeddingServiceImpl(
            @Value("${embedding.service.url:http://localhost:8000}") String embeddingServiceUrl) {
        this.embeddingUrl = embeddingServiceUrl + "/embed";
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = Map.of("inputs", List.of(text));

        EmbeddingResponse response = webClient.post()
                .uri(embeddingUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        return toFloatArray(response.embeddings().get(0));
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Map<String, Object> body = Map.of("inputs", texts);

        EmbeddingResponse response = webClient.post()
                .uri(embeddingUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
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
