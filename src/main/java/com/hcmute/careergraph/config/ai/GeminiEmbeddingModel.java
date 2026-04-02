package com.hcmute.careergraph.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class GeminiEmbeddingModel implements EmbeddingModel {

    private static final String GEMINI_EMBED_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GeminiEmbeddingModel(String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++) {
            String text = request.getInstructions().get(i);
            float[] vector = embedSingle(text);
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        return embedSingle(text);
    }

    @Override
    public float[] embed(Document document) {
        return embedSingle(document.getText());
    }

    @Override
    public int dimensions() {
        return 768;
    }

    private float[] embedSingle(String text) {
        String url = GEMINI_EMBED_URL + "?key=" + apiKey;

        Map<String, Object> body = Map.of(
            "model", "models/gemini-embedding-001",
            "content", Map.of(
                "parts", List.of(Map.of("text", text))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class
        );

        List<Double> values = (List<Double>)
            ((Map<?, ?>) response.getBody().get("embedding")).get("values");

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }
}