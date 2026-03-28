package com.hcmute.careergraph.services;

import java.util.List;

public interface EmbedService {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
