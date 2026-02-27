package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EmbedService implementation — delegates to HuggingFaceEmbeddingService
 * which calls the Python FastAPI /embed endpoint.
 */
@Service
@RequiredArgsConstructor
public class EmbedServiceImpl implements EmbedService {
    private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;

    @Override
    public float[] embed(String text) {
        return huggingFaceEmbeddingService.embed(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return huggingFaceEmbeddingService.embed(texts);
    }
}
