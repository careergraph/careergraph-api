package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import com.hcmute.careergraph.config.properties.EmbeddingRuntimeProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EmbedService implementation — delegates to HuggingFaceEmbeddingService
 * which calls the Python FastAPI /embed endpoint.
 */
@Service
@RequiredArgsConstructor
public class EmbedServiceImpl implements EmbedService {
    private static final String LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE =
            "Local embedding service unavailable and Gemini fallback is disabled.";

    private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;

    private final EmbeddingModel embeddingModel;

    private final EmbeddingRuntimeProperties embeddingRuntimeProperties;

    @Override
    public float[] embed(String text) {
        return embeddingRuntimeProperties.isUseLocalFirst() ? embedLocalFirst(text) : embedGeminiFirst(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingRuntimeProperties.isUseLocalFirst() ? embedBatchLocalFirst(texts) : embedBatchGeminiFirst(texts);
    }

    private float[] embedLocalFirst(String text) {
        try {
            return huggingFaceEmbeddingService.embed(text);
        } catch (Exception ex) {
            if (!embeddingRuntimeProperties.isAllowGeminiFallback()) {
                throw new IllegalStateException(LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE, ex);
            }
            return embeddingModel.embed(text);
        }
    }

    private List<float[]> embedBatchLocalFirst(List<String> texts) {
        try {
            return huggingFaceEmbeddingService.embed(texts);
        } catch (Exception ex) {
            if (!embeddingRuntimeProperties.isAllowGeminiFallback()) {
                throw new IllegalStateException(LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE, ex);
            }
            return embeddingModel.embed(texts);
        }
    }

    private float[] embedGeminiFirst(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception geminiEx) {
            try {
                return huggingFaceEmbeddingService.embed(text);
            } catch (Exception localEx) {
                localEx.addSuppressed(geminiEx);
                throw new IllegalStateException("Gemini-primary embedding failed and local fallback is unavailable.", localEx);
            }
        }
    }

    private List<float[]> embedBatchGeminiFirst(List<String> texts) {
        try {
            return embeddingModel.embed(texts);
        } catch (Exception geminiEx) {
            try {
                return huggingFaceEmbeddingService.embed(texts);
            } catch (Exception localEx) {
                localEx.addSuppressed(geminiEx);
                throw new IllegalStateException("Gemini-primary batch embedding failed and local fallback is unavailable.", localEx);
            }
        }
    }
}
