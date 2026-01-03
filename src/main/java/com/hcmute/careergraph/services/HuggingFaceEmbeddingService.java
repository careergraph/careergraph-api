package com.hcmute.careergraph.services;

import java.util.List;

public interface HuggingFaceEmbeddingService {
    float[] embed(String text);
    List<float[]> embed(List<String> texts);
}
