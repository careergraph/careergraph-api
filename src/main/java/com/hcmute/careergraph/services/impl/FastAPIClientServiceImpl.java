package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.request.ChatRequest;
import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;
import com.hcmute.careergraph.services.FastAPIClientService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FastAPIClientServiceImpl implements FastAPIClientService {

    private final WebClient webClient;

    @Value("${fast-api.base-url:http://localhost:8000}")
    private String FAST_API_URL;

    public ChatResponse chat(ChatRequest request) {
        try {
            log.info("Calling FastAPI chat endpoint with message: {}", request.getMessage());

            // WebClient POST request to FastAPI
            ChatResponse response = webClient.post()
                    .uri(FAST_API_URL + "/api/v1/chat")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                throw new RuntimeException("FastAPI returned null response");
            }

            return response;

        } catch (Exception ex) {
            log.error("ERROR: FastAPI chat call failed - {}", ex.getMessage());
            throw new RuntimeException("AI service temporarily unavailable", ex);
        }
    }

    @Override
    public String cvSuggestion(String prompt) {
        try {
            log.info("Calling FastAPI cv suggestion endpoint with prompt: {}", prompt);

            // WebClient POST request to FastAPI
            String response = webClient.post()
                    .uri(FAST_API_URL + "/api/v1/cv-suggestion")
                    .header("Content-Type", "application/json")
                    .bodyValue(prompt)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                throw new RuntimeException("FastAPI returned null response");
            }

            return response;

        } catch (Exception ex) {
            log.error("ERROR: FastAPI cv suggestion call failed - {}", ex.getMessage());
            throw new RuntimeException("AI service temporarily unavailable", ex);
        }
    }

    @Override
    public String reviewCvJobFit(String jsonBody) {
        try {
            log.info("Calling FastAPI review-cv endpoint");

            String response = webClient.post()
                    .uri(FAST_API_URL + "/api/v1/review-cv")
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(45))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                throw new RuntimeException("FastAPI returned null response");
            }

            return response;

        } catch (Exception ex) {
            log.error("ERROR: FastAPI review-cv call failed - {}", ex.getMessage());
            throw new RuntimeException("AI service temporarily unavailable", ex);
        }
    }

    @Override
    public String extractCvKeywords(String jsonBody) {
        try {
            log.info("Calling FastAPI extract-cv-keywords endpoint");

            String response = webClient.post()
                .uri(FAST_API_URL + "/api/v1/extract-cv-keywords")
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))
                .block();

            if (response == null) {
                throw new RuntimeException("FastAPI returned null response");
            }

            return response;

        } catch (Exception ex) {
            log.error("ERROR: FastAPI extract-cv-keywords call failed - {}", ex.getMessage());
            return null;
        }
    }
}