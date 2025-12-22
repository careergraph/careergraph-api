package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.services.EmbedService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbedServiceImpl implements EmbedService {
    private final EmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

}
